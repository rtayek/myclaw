# 🛠️ org.rtayek.myclaw.core Architectural Specification

**Framework Reference Base:** *Pattern-Oriented Software Architecture Volume 2* (POSA 2), *Enterprise Integration Patterns* (EIP), and Resiliency Engineering Patterns  
**Workspace Integration Profile:** Switchboard AX Accessible Intelligence Cockpit & Multi-Monitor Infrastructure

---

## 📋 1. Core Architectural Philosophy: Capture → Journal → Project

The **MyClaw** backend is a native, event-driven Java core that acts as an accessible **Intelligence Harness**. It captures workspace events, normalizes and fingerprints them, persists them to an authoritative append-only journal, and projects output to accessible desktop interfaces, terminals, screen readers, and speech engines.

```
       [Input Adapters]
 (Sockets, Clipboard, Process, Voice)
               │
               ▼
       [CaptureService]
 (POSA 2 Reactor Ingestion Loop)
               │
               ▼
       [Hash & Filter]
(EIP Idempotent Consumer & Time-Window Dedupe)
               │
               ▼
       [JournalService]
(POSA 2 Active Object / Append-Only JSONL Log) ◄── [Memento Snapshots]
               │
    ┌──────────┴──────────┬──────────────────────┐
    ▼                     ▼                      ▼
[Markdown Projection] [Terminal Router]   [Sonification Observer]
 (Screen Reader File)   (CQRS View)       (Audio Acoustic Cues)
```

### The Three Enduring Principles:
1. **The Journal is Authoritative:** The append-only JSON Lines (`.jsonl`) journal is the primary record of truth. All user interfaces, transcripts, and audio streams are **replayable projections** derived from journaled events.
2. **Context & Data Sovereignty:** Events are captured at transaction time on local disk. Rehydrating local context enables seamless switching across AI backends without cloud vendor lock-in.
3. **Accessibility Without Lag:** Input capture and persistence live on dedicated non-blocking threads, ensuring that typing, screen-reader text rendering, and speech synthesis never stutter or freeze.

---

## 🛠️ 2. Architectural Pattern Taxonomy

The backend design combines eight foundational software design patterns:

### 1. Ingestion Layer: POSA 2 Reactor Pattern (§2.3)
* **Role:** Demultiplexes concurrent asynchronous input channels (`ServerSocketChannel`, Subprocess stdout, Clipboard, Voice) via a single non-blocking `java.nio.channels.Selector` loop.
* **Impact:** Eliminates typing lag and UI freeze by keeping payload extraction off the main user/UI thread.

### 2. Adaptation Layer: Pluggable CaptureAdapter Pattern
* **Role:** Standardizes disparate event sources (sockets, stdin, clipboard changes, global hotkeys, file watchers) into a unified `WorkspaceEvent` stream.
* **Adapters:** `SocketCaptureAdapter`, `ProcessCaptureAdapter`, `ClipboardCaptureAdapter`, `VoiceCaptureAdapter`, `FileWatchCaptureAdapter`.

### 3. Filtration Layer: EIP Idempotent Consumer & Bounded Deduplication
* **Role:** Cryptographically calculates an SHA-256 fingerprint for canonical payloads.
* **Deduplication Policy:** Filters repetitive compiler diagnostics or system notifications within a configurable time window (e.g., 5s), collapsing noise without discarding distant distinct events.

### 4. Resiliency Layer: Circuit Breaker Pattern
* **Role:** Monitors external cloud API calls (Claude, OpenAI, remote servers).
* **Impact:** If a remote cloud API times out, rate-limits, or loses network connectivity, the Circuit Breaker trips instantly to `OPEN` state and seamlessly redirects execution to the local deskside model (`Ollama / GLM-4 9B`) without UI dialog crashes.

### 5. Routing Layer: Chain of Responsibility + Strategy Pattern
* **Role:** Implements the **Model Routing Engine** to optimize **Token Value per Watt**.
* **Chain Evaluators:**
  * `AirgapCheckStrategy` → If airgapped mode is forced, route to Ollama immediately.
  * `ComplexityEvaluatorStrategy` → Simple tasks, code reviews, and short scripts run locally at zero token cost.
  * `FrontierEscalationStrategy` → Complex multi-step reasoning escalates to remote frontier models (Claude/Gemini) as sub-agents.

### 6. Persistence Layer: POSA 2 Active Object Pattern (§2.6) & Memento
* **Role:** Isolates file I/O operations onto a single background thread pool running at `Thread.MIN_PRIORITY`.
* **Snapshotting (Memento):** Periodically writes a lightweight session state snapshot (`SessionMemento`) so startup rehydration occurs instantly without re-parsing millions of historical log lines from scratch.

### 7. Presentation Layer: CQRS (Command Query Responsibility Segregation)
* **Role:** Separates write operations (event capture & JSONL journaling) from read projections (human-scannable Markdown, Windows Terminal views, Web Cockpit).

### 8. Acoustic Layer: Decoupled Sonification Observer & Null Object Fallback
* **Role:** Publishes distinct audio status cues (`EVENT_JOURNALED`, `BUILD_FAILED`, `RESPONSE_COMPLETE`) via a rate-limited event bus.
* **Null Object Fallback:** If audio hardware or speech synthesis drivers are unavailable, a `NullSpeechAdapter` degrades silently to text-only logging without throwing `NullPointerException`.

### 9. Display Infrastructure: Dynamic Topology Manager & Spatial Router (0 to 10+ Monitors)
* **Role:** Adapts projection routing dynamically based on physical monitor count detected at runtime (`GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()`):
  * **0 Monitors (Headless / Daemon Mode):** Serves CLI, TCP socket pipes, SSH sessions, background JSONL journaling, and audio sonification without creating any GUI windows (`GraphicsEnvironment.isHeadless()`).
  * **1 Monitor (Single Cockpit View):** All projections (transcript, controls, telemetry, diagnostics) collapse into an integrated tabbed / split-pane single window.
  * **2 to 10+ Monitors (Spatial Multi-Screen Infrastructure):** Projections are dynamically cast onto dedicated physical screens `[0..N-1]` (e.g., Screen 0 = IDE Cockpit, Screen 1 = Full-screen Markdown Transcript for Magnifiers, Screen 2 = Telemetry & Diagnostic Log, Screen 3..10 = Floating Inspection Monitors).
* **Graceful Fallback:** Disconnecting a monitor instantly re-docks floating windows to remaining active screens without state loss.

---

## 📦 3. Core Java Architectural Reference Code (JDK 21+)

```java
package org.rtayek.myclaw.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

// ==========================================================================
// 1. EVENT IDENTITY & ENVELOPE
// ==========================================================================
public enum EventSource {
    VOICE, CLIPBOARD, COMPILER_ALERT, OLLAMA_STREAM, CLAUDE_STREAM, SYSTEM
}

public enum EventKind {
    PROMPT, RESPONSE, DIAGNOSTIC, TELEMETRY, CONTROL
}

public record WorkspaceEvent(
    UUID eventId,
    long sequence,
    Instant occurredAt,
    EventSource source,
    EventKind kind,
    String workspaceId,
    String sessionId,
    UUID correlationId,
    String contentHash,
    byte[] payload
) {
    public WorkspaceEvent(EventSource source, EventKind kind, String workspaceId, String sessionId, byte[] payload, String contentHash) {
        this(UUID.randomUUID(), System.currentTimeMillis(), Instant.now(), source, kind, workspaceId, sessionId, null, contentHash, payload);
    }
}

// ==========================================================================
// 2. CIRCUIT BREAKER RESILIENCY PATTERN
// ==========================================================================
public class BackendCircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private State state = State.CLOSED;
    private int failureCount = 0;
    private final int threshold = 3;
    private long lastStateChange = System.currentTimeMillis();
    private final long resetTimeoutMs = 10_000; // 10s fallback window

    public synchronized boolean allowRequest() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastStateChange > resetTimeoutMs) {
                state = State.HALF_OPEN;
                return true;
            }
            return false; // Trip to local fallback
        }
        return true;
    }

    public synchronized void recordSuccess() {
        failureCount = 0;
        state = State.CLOSED;
    }

    public synchronized void recordFailure() {
        failureCount++;
        if (failureCount >= threshold) {
            state = State.OPEN;
            lastStateChange = System.currentTimeMillis();
        }
    }

    public State getState() { return state; }
}

// ==========================================================================
// 3. EIP: IDEMPOTENT CONSUMER & CONTENT HASHER
// ==========================================================================
public class EventHasher {
    public static String computeSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

// ==========================================================================
// 4. POSA 2: ACTIVE OBJECT ASYNCHRONOUS JOURNALER
// ==========================================================================
public class AsynchronousJournaler implements AutoCloseable {
    private final ExecutorService persistenceThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SovereignPersistenceThread");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private final Path jsonlPath;
    private final Path transcriptDir;

    public AsynchronousJournaler(Path jsonlPath, Path transcriptDir) {
        this.jsonlPath = jsonlPath;
        this.transcriptDir = transcriptDir;
    }

    public void enqueue(WorkspaceEvent event) {
        persistenceThread.submit(() -> {
            writeJsonl(event);
            writeMarkdownProjection(event);
        });
    }

    private synchronized void writeJsonl(WorkspaceEvent event) {
        try (BufferedWriter writer = Files.newBufferedWriter(jsonlPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String record = String.format("{\"id\":\"%s\",\"seq\":%d,\"time\":\"%s\",\"src\":\"%s\",\"kind\":\"%s\",\"hash\":\"%s\"}\n",
                event.eventId(), event.sequence(), event.occurredAt(), event.source(), event.kind(), event.contentHash());
            writer.write(record);
        } catch (IOException e) {
            System.err.println("Journal write failed: " + e.getMessage());
        }
    }

    private void writeMarkdownProjection(WorkspaceEvent event) {
        Path mdFile = transcriptDir.resolve("session-" + event.sessionId() + ".md");
        try (BufferedWriter writer = Files.newBufferedWriter(mdFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(String.format("### [%s] %s (%s)\n", event.occurredAt(), event.source(), event.kind()));
            writer.write(new String(event.payload(), StandardCharsets.UTF_8));
            writer.newLine();
            writer.newLine();
        } catch (IOException ignored) {}
    }

    @Override
    public void close() {
        persistenceThread.shutdown();
        try {
            if (!persistenceThread.awaitTermination(2, TimeUnit.SECONDS)) {
                persistenceThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            persistenceThread.shutdownNow();
        }
    }
}
```

---

## 📁 4. Package Structure Definition

```text
org.rtayek.myclaw.core
├── backend/
│   ├── BackendLifecycle.java
│   ├── BackendHealth.java
│   └── HarnessMain.java
├── capture/
│   ├── CaptureAdapter.java
│   ├── SocketCaptureAdapter.java
│   ├── ProcessCaptureAdapter.java
│   ├── ClipboardCaptureAdapter.java
│   └── VoiceCaptureAdapter.java
├── event/
│   ├── WorkspaceEvent.java
│   ├── EventSource.java
│   ├── EventKind.java
│   └── EventHasher.java
├── journal/
│   ├── EventJournal.java
│   ├── JsonlEventJournal.java
│   ├── AsynchronousJournaler.java
│   └── SessionMemento.java
├── routing/
│   ├── ModelRouter.java
│   ├── AirgapRoutingStrategy.java
│   └── TokenValueWattEvaluator.java
├── resilience/
│   ├── BackendCircuitBreaker.java
│   └── LocalFallbackHandler.java
└── projection/
    ├── EventProjector.java
    ├── MarkdownTranscriptProjector.java
    ├── TerminalProjector.java
    └── SonificationObserver.java
```

---

## 🎯 5. Target Implementation & Verification Milestones

1. **Synchronous JSONL Journal Core:** Implement and test append-only journaling with recovery for truncated last lines.
2. **Replay Engine (`EventReplayer`):** Verify rebuilding `.md` transcripts from historical `.jsonl` journals.
3. **Multi-Adapter Capture Pipeline:** Verify socket (`7701-7704`), stdin, and clipboard event ingestion under concurrent load.
4. **Resilience & Circuit Breaker Verification:** Test sub-second fallback from Claude API to local Ollama (`GLM-4 9B`) on simulated network failure.
5. **Sonification & Speech Cues:** Validate rate-limited acoustic cues and `NullSpeechAdapter` fallback.