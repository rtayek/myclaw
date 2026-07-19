# MyClaw Backend

## Purpose

The MyClaw backend is the native Java core beneath MyClaw / Switchboard AX. It captures workspace events, normalizes and identifies them, persists them durably, and routes their output to deterministic terminal or other presentation views.

The backend must not assume a fixed physical display layout. The app may run headless, on a single-screen laptop, or on a workstation with many monitors. A useful design target is zero to ten attached displays.

The core pipeline is:

```text
Capture → Normalize → Identify → Persist → Route → Present
```

A stronger architectural name for the backend is:

```text
Capture → Journal → Project
```

Hashing is an implementation mechanism. The append-only journal and its replayable projections are the enduring backend concepts.

---

## Core Product Idea

The strongest idea is not SHA-256 or Java NIO. It is that every interaction becomes a durable event before presentation.

That provides:

- replayable history,
- model-independent transcripts,
- deterministic terminal output,
- deduplication and correlation,
- recovery after crashes,
- a backend usable by multiple agent harnesses,
- clean separation between event capture and presentation.

The durable event spine may be the principal product differentiator.

---

## Architectural Responsibilities

The backend should own:

```text
capture adapters
event normalization
event identity and correlation
append-only persistence
replay
deduplication policy
routing decisions
projection delivery
health and failure reporting
```

Terminal windows, voice interfaces, OpenClaw, Ollama, cloud models, Eclipse integration, browser clients, and other user interfaces should be replaceable front ends or adapters.

Display count is environmental state, not architecture. Routing should target named presentation roles or capabilities, then bind those roles to whatever displays, windows, files, audio devices, or headless outputs are available.

---

## Recommended Architecture

```text
CaptureAdapter
    ↓
EventNormalizer
    ↓
EventJournal
    ↓
EventBus
   ↙  ↓  ↘
Router  TranscriptProjector  SonificationProjector
```

The journal is authoritative.

Terminal output, Markdown transcripts, sound cues, browser views, and other interfaces are projections derived from journaled events.

---

## Pattern Map

The backend should use patterns as vocabulary for design pressure, not as decorative labels.

The useful pattern stack is:

```text
Ports and Adapters
    → Event Sourcing journal
    → CQRS projections
    → bounded async workers with backpressure
```

### Event Sourcing

The append-only journal is the source of truth. Current terminal output, Markdown transcripts, search indexes, audio cues, summaries, and future dashboard views are derived state.

This matters because projections can be rebuilt when rendering rules improve, when accessibility needs change, or when a crash interrupts output delivery.

### Ports and Adapters

Capture sources and AI providers should sit behind stable internal interfaces.

Examples:

- sockets,
- subprocess streams,
- clipboard,
- global hotkeys,
- voice APIs,
- filesystem watchers,
- Claude CLI,
- Ollama,
- cloud APIs.

No source should force the rest of the backend into its native shape.

### CQRS

The write side captures, normalizes, and journals events.

The read side serves projections:

- Markdown transcript files,
- terminal views,
- audio cues,
- search indexes,
- session dashboards,
- health views.

This keeps capture correctness separate from presentation convenience.

### Outbox

If a journaled event must later be projected to terminals, Markdown, audio, or search, that projection work should be recoverable.

The simplest form is a projection checkpoint per projector. A stronger later form is an explicit outbox of pending projection tasks. Either way, the system should avoid this failure mode:

```text
event appended successfully
    ↓
process crashes before transcript projection
    ↓
transcript silently misses data
```

Replay can repair the projection, but the backend should make missed projection work detectable.

### Strategy

Routing, deduplication, redaction, transcript rendering, import parsing, and model-selection policy should be explicit strategies with tests. These are policies, not hardcoded facts.

### Circuit Breaker

External or unstable components should expose degraded state instead of being hammered indefinitely.

Candidates include:

- local model servers,
- cloud APIs,
- speech services,
- terminal projection targets,
- filesystem or journal targets.

Circuit breakers should produce health events that can be projected somewhere other than the failing component.

### Memento

Window layout, selected views, zoom, routing preferences, available display inventory, and session workspace state are mementos. They should be stored separately from the original event record so restoring a workspace never rewrites history.

---

## Capture Layer

A Java `Selector` is appropriate for selectable channels such as sockets and pipes. It should not define the entire ingestion architecture.

Clipboard changes, global hotkeys, filesystem events, subprocess callbacks, and voice APIs arrive through different mechanisms.

Use a common capture interface with separate adapters:

```text
SocketCaptureAdapter
ProcessCaptureAdapter
ClipboardCaptureAdapter
HotkeyCaptureAdapter
VoiceCaptureAdapter
FileWatchCaptureAdapter
```

A selector-driven NIO reactor may power `SocketCaptureAdapter`, but the rest of the system should not be forced into a socket-shaped abstraction merely because engineers enjoy making unrelated things wear matching uniforms.

### Suggested Interface

```java
interface CaptureAdapter extends AutoCloseable {
    void start(EventSink sink);
    CaptureStatus status();
}
```

```java
interface EventSink {
    void accept(CapturedEvent event);
}
```

Each adapter should translate its native callback, stream, process, or watcher mechanism into a common captured-event format.

---

## Event Identity

Event identity and content identity are different concepts.

An event should include:

- `eventId`: identifies one occurrence,
- `contentHash`: identifies equivalent content,
- `correlationId`: connects related events,
- `causationId`: identifies the event that caused another event,
- `sequence`: establishes ordering,
- `occurredAt`: when the source event occurred,
- `recordedAt`: when the backend accepted or journaled it.

SHA-256 should not be the primary identity of an event.

### Recommended Event Envelope

```java
record WorkspaceEvent(
        UUID eventId,
        long sequence,
        Instant occurredAt,
        Instant recordedAt,
        EventSource source,
        String workspaceId,
        String sessionId,
        UUID correlationId,
        UUID causationId,
        EventKind kind,
        byte[] payload,
        String contentHash,
        Map<String, String> attributes) {
}
```

Long term, `byte[]` should not be the only meaningful payload representation. The backend will likely need typed event bodies or a versioned envelope.

A possible versioned structure is:

```java
record EventEnvelope(
        int schemaVersion,
        WorkspaceEventMetadata metadata,
        EventPayload payload) {
}
```

---

## Event Normalization

Captured events should be normalized before hashing, deduplication, or routing.

Normalization may include:

- character encoding conversion,
- newline normalization,
- removal of unstable timestamps from selected diagnostics,
- source-specific metadata extraction,
- event-kind classification,
- workspace and session association,
- optional sensitive-data filtering,
- canonical payload serialization.

Normalization rules must be explicit and testable. Otherwise two visually identical compiler errors can hash differently because one contains `\r\n`, and computers will once again demonstrate their devotion to technically correct inconvenience.

---

## Hashing

SHA-256 is suitable for content fingerprints.

The hash should be calculated from a canonical representation, not blindly from arbitrary source bytes.

Example:

```java
final class ContentHasher {
    String hash(byte[] canonicalPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalPayload));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
```

The content hash is useful for:

- duplicate detection,
- payload integrity checks,
- content-addressed lookup,
- repeated-event aggregation,
- projection caching.

It should not replace the event ID.

---

## Deduplication

Deduplication requires a policy. It should not be implemented as a permanent in-memory set of all hashes ever observed.

A useful policy might be:

```text
same normalized content
+ same source
+ same workspace
+ same event kind
+ within a configured time window
```

For example:

```text
same compiler diagnostic
+ same project
+ within 5 seconds
```

Repeated events may be better collapsed than discarded:

```text
Compiler error repeated 37 times between 12:04:10 and 12:04:18
```

### Suggested Abstraction

```java
interface DuplicatePolicy {
    DuplicateDecision evaluate(WorkspaceEvent event);
}
```

```java
sealed interface DuplicateDecision {
    record Accept() implements DuplicateDecision {}
    record Suppress(UUID originalEventId) implements DuplicateDecision {}
    record Aggregate(UUID originalEventId, long repetitionCount) implements DuplicateDecision {}
}
```

The implementation should use a bounded cache with expiration. It should not grow forever, and important duplicate state should not disappear unpredictably after restart unless that behavior is intentional.

---

## Persistence

Persistence is the critical boundary of the system.

The backend should not silently ignore persistence failures.

This is unacceptable:

```java
catch (IOException ignored) {
}
```

For a system whose purpose is capture and persistence, silent data loss is nearly the worst possible behavior.

Persistence failures should produce:

- a visible diagnostic event,
- retry or quarantine behavior,
- health status,
- failure counters,
- an emergency fallback journal where practical,
- orderly degradation when the primary journal is unavailable.

### Journal Contract

```java
interface EventJournal extends AutoCloseable {
    JournalPosition append(WorkspaceEvent event);
    Stream<WorkspaceEvent> readFrom(JournalPosition position);
    JournalHealth health();
}
```

### Initial Storage Format

Start with append-only JSON Lines:

```json
{"schemaVersion":1,"sequence":42,"eventId":"...","recordedAt":"...","source":"COMPILER","kind":"DIAGNOSTIC","contentHash":"...","payload":"..."}
```

Each line should contain a complete event envelope.

The initial implementation should favor correctness and recoverability over clever concurrency.

### File Handling Requirements

The journal should:

- create parent directories,
- use UTF-8 explicitly,
- append complete records,
- flush according to a documented durability policy,
- recover from a partially written final line,
- expose its current sequence,
- prevent multiple accidental writers,
- rotate files when needed,
- retain schema-version information,
- verify parsing during replay.

---

## Projection Delivery and Outbox

Projection delivery should be recoverable.

At minimum, each projector should record the last journal position it fully applied. On restart, it should resume from that position and rebuild or catch up.

For projections that have side effects outside the journal, such as terminal writes, audio cues, notifications, or external indexes, consider a small outbox:

```java
interface ProjectionOutbox {
    void enqueue(ProjectionTask task);
    Stream<ProjectionTask> pendingFor(ProjectionName projector);
    void markCompleted(ProjectionTaskId id);
    void markFailed(ProjectionTaskId id, ProjectionFailure failure);
}
```

The first implementation can avoid a formal outbox if replay plus projector checkpoints are enough. The important rule is that projection failure must be observable and repairable.

---

## Asynchronous Persistence

Asynchronous persistence should be added after the synchronous journal is proven correct.

Use a bounded queue, not an unlimited executor queue.

```java
final class AsyncEventJournal implements EventJournal {
    private final BlockingQueue<PendingAppend> pendingAppends;
    private final Thread writerThread;
}
```

The asynchronous layer needs an explicit backpressure policy.

Possible policies include:

- block the producer,
- reject low-priority events,
- spool temporarily to a fallback file,
- aggregate repetitive diagnostics,
- report overload as a health event.

Dropping events silently is not a policy. It is merely data loss wearing business-casual clothing.

`Thread.MIN_PRIORITY` should not be relied on as the primary resource-control mechanism. Queue bounds, batching, I/O behavior, and backpressure matter more than thread priority.

---

## Replay

Replay is a first-class backend capability, not an optional debugging trick.

The backend should support:

- replaying all events,
- replaying from a sequence number,
- replaying one workspace or session,
- rebuilding Markdown transcripts,
- rebuilding terminal projections,
- testing routing rules against historical data,
- recovering projections after failure.

Example:

```java
interface EventReplayer {
    void replay(EventQuery query, EventConsumer consumer);
}
```

Replay enables the system to evolve without discarding old sessions whenever a projection format changes.

---

## Routing

Routing should not be embedded in hashing.

Hashing identifies content. Routing decides which projection or view receives an event.

### Suggested Model

```java
interface RoutingRule {
    boolean matches(WorkspaceEvent event);
    Set<RouteTarget> targetsFor(WorkspaceEvent event);
}
```

```java
enum RouteTarget {
    PRIMARY_CONVERSATION,
    DIAGNOSTICS,
    RAW_EVENTS,
    REFERENCE,
    STATUS,
    TRANSCRIPT,
    AUDIO,
    JOURNAL_ONLY
}
```

Routing may consider:

- workspace,
- session,
- source,
- event kind,
- severity,
- model,
- correlation ID,
- user-selected focus,
- accessibility priority,
- display inventory,
- projection availability.

Routing results should themselves be inspectable and testable.

A routing decision may also be journaled if exact historical presentation behavior matters.

### Display-Agnostic Routing

Do not hardcode routes to physical monitor positions.

The backend should route events to semantic presentation roles:

```text
conversation
diagnostics
raw events
reference material
status
audio
transcript
journal only
```

A separate layout binder maps those roles onto the current environment.

Examples:

```text
0 displays:
    journal + transcript + audio or remote/API view if available

1 display:
    tabbed or split-pane roles

2 displays:
    primary conversation on one display, diagnostics/status on another

3+ displays:
    user-defined role-to-window placement

10 displays:
    still the same roles; more physical surfaces do not create new backend concepts
```

If a route target is unavailable, routing should degrade explicitly:

- project to the journal,
- project to the transcript,
- raise a health event,
- use the next configured presentation role,
- never drop an event silently because a monitor or terminal window is missing.

---

## Transcript Projection

Markdown transcript files should be human-oriented.

They should not be named solely by content hash.

A better structure is:

```text
transcripts/
  2026-07-18/
    session-openclaw-main.md
    build-tinyllm.md
    myclaw-development.md
```

The journal retains exact event IDs, hashes, and metadata internally.

A transcript projector may render events as:

```markdown
## 12:04:18 — Compiler diagnostic

Workspace: `myclaw`
Source: `COMPILER`
Event: `...`

```text
error: cannot find symbol
```
```

The projector should support rebuilding transcript files from the journal.

### Suggested Interface

```java
interface EventProjector {
    ProjectionName name();
    void apply(WorkspaceEvent event);
    void rebuild(Stream<WorkspaceEvent> events);
}
```

---

## Sonification Projection

Audio cues should subscribe to committed or otherwise well-defined transactional points.

Possible cues include:

- event captured,
- event durably journaled,
- model response completed,
- build succeeded,
- build failed,
- persistence degraded,
- queue overload,
- terminal route unavailable.

Avoid making every token beep unless the intended product direction is to recreate a 1980s modem inside the user’s office.

Sonification rules should be independently configurable and disabled by default where appropriate.

---

## Health and Diagnostics

The backend should expose health for:

- capture adapters,
- normalization,
- journal availability,
- queue depth,
- projection status,
- routing failures,
- dropped or aggregated event counts,
- replay progress.

Example:

```java
record BackendHealth(
        HealthStatus overall,
        Map<String, ComponentHealth> components,
        long lastCommittedSequence,
        int pendingJournalWrites) {
}
```

Health failures should be visible without depending on the failing projection.

For example, a transcript-write failure should still be reportable through the terminal diagnostic view and journal health status.

External backends and projection targets should also expose circuit-breaker state:

- closed: normal operation,
- open: failing quickly after repeated errors,
- half-open: probing recovery with limited traffic.

Circuit-breaker transitions should become journaled or health-visible events so the user can understand why work was delayed, rerouted, or skipped.

---

## Workspace State

Window layout and user presentation state should be treated as mementos, not as source events.

Examples:

- window bounds,
- monitor assignment,
- display inventory at last save,
- selected session,
- active transcript view,
- font and zoom settings,
- muted or enabled audio cues,
- preferred route targets.

This state should be restorable and user-editable, but it should not alter the original event journal. A session record says what happened. A workspace memento says how the user preferred to see it.

Layout restore should be forgiving:

- if no display is available, keep the workspace reachable through journal, transcript, audio, or remote control paths;
- if fewer displays are available than before, collapse missing windows into tabs or a window list;
- if more displays are available, do not automatically scatter windows without user preference;
- if a monitor identity changes, match by stable hints when possible and fall back predictably.

---

## Shutdown Semantics

Every asynchronous component must have explicit shutdown behavior.

Shutdown should:

1. stop accepting new captures,
2. finish or reject pending normalization work,
3. drain the persistence queue,
4. flush the journal,
5. close projections,
6. close capture adapters,
7. report whether shutdown completed cleanly.

Use structured lifecycle objects rather than scattered shutdown hooks.

```java
interface BackendLifecycle extends AutoCloseable {
    void start();
    BackendState state();
    void stop(Duration timeout);
}
```

---

## Recommended Package Structure

```text
org.rtayek.myclaw.backend
    Backend
    BackendLifecycle
    BackendHealth

org.rtayek.myclaw.capture
    CaptureAdapter
    SocketCaptureAdapter
    ProcessCaptureAdapter
    ClipboardCaptureAdapter
    HotkeyCaptureAdapter
    VoiceCaptureAdapter
    FileWatchCaptureAdapter

org.rtayek.myclaw.event
    WorkspaceEvent
    EventEnvelope
    EventSource
    EventKind
    EventNormalizer
    ContentHasher

org.rtayek.myclaw.journal
    EventJournal
    JsonlEventJournal
    AsyncEventJournal
    JournalPosition
    JournalHealth
    EventReplayer

org.rtayek.myclaw.deduplication
    DuplicatePolicy
    DuplicateDecision
    TimeWindowDuplicatePolicy

org.rtayek.myclaw.routing
    EventRouter
    RoutingRule
    RouteTarget
    RoutingDecision
    PresentationRole

org.rtayek.myclaw.projection
    EventProjector
    ProjectionOutbox
    ProjectionCheckpointStore
    MarkdownTranscriptProjector
    TerminalProjector
    SonificationProjector

org.rtayek.myclaw.workspace
    WorkspaceMemento
    LayoutStore
    ViewState
    DisplayInventory
    LayoutBinder

org.rtayek.myclaw.resilience
    CircuitBreaker
    CircuitBreakerState
    ComponentFailure
```

Package boundaries should reflect stable responsibilities, not individual design-pattern names.

---

## Implementation Order

1. Define the event envelope and journal contract.
2. Implement a synchronous append-only JSONL journal.
3. Add journal round-trip tests.
4. Add recovery tests for a truncated final record.
5. Add replay by sequence, workspace, and session.
6. Implement one simple capture adapter, preferably subprocess or socket input.
7. Add event normalization and content hashing.
8. Add routing as a journal-driven projection.
9. Add Markdown transcript projection.
10. Add projector checkpoints so projections can catch up after restart.
11. Add asynchronous persistence with a bounded queue and explicit backpressure.
12. Add deduplication after observing real duplicate behavior.
13. Add health reporting and circuit-breaker state for external components.
14. Add terminal projections.
15. Add workspace memento storage.
16. Add sonification last.

This order protects the durable core before adding concurrency. Clever concurrency has an uncanny ability to transform missing requirements into missing data.

---

## Initial Tests

The tests should act as the functional specification.

### Event Tests

- event IDs are unique,
- equal payloads may have equal content hashes but different event IDs,
- canonical newline forms produce the same hash,
- event sequence increases monotonically.

### Journal Tests

- appending one event creates one readable record,
- appending many events preserves order,
- payloads round-trip exactly,
- restart preserves the next sequence,
- a truncated final line does not corrupt earlier records,
- unsupported schema versions fail visibly,
- write failures appear in journal health.

### Replay Tests

- replay from the beginning returns all events,
- replay from a sequence returns only later events,
- workspace filtering is correct,
- session filtering is correct,
- projector rebuild matches live projection,
- projector checkpoints resume from the correct journal position.

### Deduplication Tests

- identical compiler events inside the window aggregate or suppress,
- identical compiler events outside the window are accepted,
- identical content from different workspaces is accepted,
- deduplication state remains bounded.

### Routing Tests

- diagnostics reach the configured diagnostic presentation role,
- model output reaches the configured conversation presentation role,
- journal failures reach the diagnostic route,
- unmatched events follow a documented default route,
- routes target presentation roles rather than fixed monitor numbers,
- missing displays produce fallback projections rather than dropped events.

### Lifecycle Tests

- shutdown drains accepted journal writes,
- new events are rejected after shutdown begins,
- capture adapters close,
- projectors close,
- timeout produces an unhealthy shutdown result.

### Resilience Tests

- a failing projection records a visible health event,
- a restarted projector catches up from its checkpoint,
- repeated backend failures open the circuit breaker,
- half-open recovery restores normal operation after a successful probe,
- window layout restore does not modify journaled events,
- a layout saved with many displays remains usable with one display,
- headless operation still journals and projects to non-display outputs.

---

## First Concrete Milestone

The first useful backend milestone should be deliberately small:

```text
stdin or socket input
    ↓
normalize UTF-8 text
    ↓
create WorkspaceEvent
    ↓
append JSONL journal
    ↓
project to one Markdown transcript
```

Acceptance criteria:

- every accepted input appears in the journal,
- every journaled input appears in the transcript,
- restarting the process preserves history,
- replay rebuilds the transcript,
- persistence errors are visible,
- tests cover ordering and recovery.

Once that works, terminal routing and additional adapters can be added without weakening the core.

---

## Original Handoff Summary

The backend was initially described as a thread-isolated Capture-Hash-Persist pipeline:

- `CaptureService`: Reactor-style ingestion using Java NIO selectors,
- `HashService`: SHA-256 fingerprinting, idempotent-consumer behavior, and content-based routing,
- `PersistenceService`: asynchronous JSONL journaling and Markdown transcript writing,
- future spatial routing to fixed terminal views,
- future audio sonification.

That direction is useful, with the following corrections:

- the Reactor belongs inside selected capture adapters, not across every event source,
- hashes identify content, not event occurrences,
- deduplication requires scope and expiration,
- persistence failures must not be ignored,
- Markdown transcript names should be human-readable,
- routing is separate from hashing,
- fixed terminal views should become display-agnostic presentation roles,
- the journal should be authoritative,
- terminal, Markdown, and audio outputs should be replayable projections.

---

## Design Conclusion

The MyClaw backend should be designed as a durable event system:

```text
Capture → Normalize → Journal → Replayable Projections
```

The journal is the source of truth.

Hashes, routing, Markdown files, terminal windows, and audio cues are supporting mechanisms around that source of truth.

This gives MyClaw a backend that is:

- accessible,
- deterministic,
- inspectable,
- replayable,
- model-independent,
- recoverable,
- usable by multiple interfaces and agent harnesses.
