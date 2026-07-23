# myclaw Backend — Task 1 Implementation Record

**Project:** OpenClaw / myclaw (Switchboard AX Backend Core)
**Status:** Task 1 (Native Java NIO Reactor Loop / `CaptureService`) complete and verified against a live socket smoke test on JDK 21.

---

## Deliverables

| File | Role |
|---|---|
| `CaptureService.java` | POSA 2 Reactor (§2.3) ingestion loop |
| `Core.java` | Reference classes: `WorkspaceEvent`, `EventSource`, `WorkspaceEventHandler`, `HashRoutingService` (EIP Idempotent Consumer), `AsynchronousJournaler` (POSA 2 Active Object / EIP Message Store) |
| `Main.java` | Pipeline wiring: Capture → Hash → Persist, with shutdown hook |

---

## CaptureService — the Reactor (POSA 2 §2.3)

One `java.nio.channels.Selector` thread demultiplexes all four loopback ports:

- **7701** — VOICE
- **7702** — CLIPBOARD
- **7703** — COMPILER_ALERT
- **7704** — OLLAMA_STREAM

`AcceptorHandler` and `ChannelReadHandler` are the concrete event handlers, attached via `SelectionKey.attachment()`, so the dispatch loop is a single virtual call with no `instanceof` chains.

Producers can be as simple as:

```
some-daemon | nc 127.0.0.1 7703
```

### Key design details

1. **Newline framing with a growable per-channel buffer.** Events reach `HashRoutingService` as complete text blocks, never torn mid-line. This matters both for stable SHA-256 dedupe signatures and for screen-reader-clean output units. A 1 MiB per-line guard prevents a runaway producer from ballooning memory. Partial frames are flushed on producer disconnect; `\r` is stripped for Windows producers.

2. **Cross-thread safety.** `submitToReactor()` queues commands into a `ConcurrentLinkedQueue` and calls `Selector.wakeup()`, so later components (the spatial routing engine, sonification hooks) can safely register new channels without hitting the classic `register()`/`select()` deadlock.

3. **Fault isolation.** A per-channel `IOException` closes just that channel; the harness keeps serving the others. A misbehaving producer can never take down the Reactor.

4. **No blocking I/O on the reactor thread.** All channels are non-blocking; heavy disk I/O lives exclusively on the Active Object thread (`SovereignPersistenceThread`, `Thread.MIN_PRIORITY`), so the Center Monitor IDE workspace never stutters.

### Lifecycle

- `start()` — binds all `ServerSocketChannel` acceptors on 127.0.0.1, registers `OP_ACCEPT`, launches the `myclaw-reactor` thread.
- `run()` — the event loop: `select()` → drain pending cross-thread commands → dispatch ready keys to attached handlers.
- `close()` — flips the running flag, wakes the selector, joins the thread (2 s bound), closes every channel, closes the selector.

---

## Core.java notes

The reference classes from the handoff document are included as-is, with one improvement: `AsynchronousJournaler` now implements `AutoCloseable` so shutdown drains the persistence queue cleanly (`shutdown()` + bounded `awaitTermination`) instead of dropping enqueued writes.

Pipeline contract: `HashRoutingService.handleEvent()` is cheap by design — it only hashes and enqueues. The Idempotent Consumer check drops duplicate `COMPILER_ALERT` signatures before they ever reach disk.

**Known defect, not yet fixed here:** the write paths in `AsynchronousJournaler` (`writeToJSONL`, `writeToMarkdownMirror`) catch `IOException` and drop it. For a system whose entire purpose is durable capture, a silently lost write is closer to the worst possible failure mode than a crash would be — a crash is at least visible. Before this moves past Task 1, persistence failures need to surface as a visible diagnostic/health signal (see `openai-backend.md`'s `JournalHealth` / `BackendHealth` treatment) rather than being swallowed.

**Known conflation, not yet fixed here:** the SHA-256 signature in `HashRoutingService` is doing two jobs — content dedup key and de facto event identity (it's the filename for the transcript mirror and the dedup key in `journal.jsonl`). Two occurrences of literally identical text (e.g. the same voice command spoken twice, deliberately) will collapse into one record for anything but `COMPILER_ALERT`, or collide on the same transcript filename. A real `eventId` (UUID, already present on `WorkspaceEvent` but currently unused downstream) should be the record identity; the content hash should stay scoped to what it's actually good for — dedup and integrity checks.

## Main.java wiring

```
CaptureService (Reactor, 4 ports)
      ↓ WorkspaceEvent (newline-framed)
HashRoutingService (SHA-256 + idempotent dedupe)
      ↓ enqueue
AsynchronousJournaler (Active Object, MIN_PRIORITY thread)
      ↓
journal.jsonl (append-only sync journal)
transcripts/<hash>.md (screen-reader/magnifier-scannable mirrors)
```

A JVM shutdown hook closes the capture service first, then the journaler, guaranteeing ordered teardown.

---

## Smoke test results (JDK 21, OpenJDK 21.0.10)

Test: piped one VOICE line, **three identical** COMPILER_ALERT lines, and one OLLAMA_STREAM line over TCP sockets.

Result:
- `journal.jsonl` contains exactly **3 records** — the idempotent consumer collapsed the duplicate alerts into one.
- `transcripts/` contains exactly **3 markdown files**, one per unique signature, each holding the clean payload text.

```
{"hash":"2c7975e8...","source":"VOICE"}           → "open the build panel"
{"hash":"2076dd72...","source":"COMPILER_ALERT"}  → "ERROR: cannot find symbol Foo"  (x3 collapsed to 1)
{"hash":"5cac4f98...","source":"OLLAMA_STREAM"}   → "The quick brown fox"
```

---

## Next tasks (seams already in place)

2. **Spatial Routing Engine** — slots in as another `WorkspaceEventHandler` that `HashRoutingService` fans out to after dedupe; maps content properties onto individual physical terminal views.
3. **Audio Sonification Integration** — hooks naturally onto the journaler's enqueue/complete transactional endpoints; new channels can be registered live via `submitToReactor()`.

## Scope note

This document and its "Switchboard AX" three-monitor / spatial-routing / sonification framing come from a separate handoff than `claudes-vision.md`'s "Manifold Switchboard" — a general multi-window cockpit over many AI backends, organized around sessions and projects, not a specific three-monitor accessibility capture rig. Both are plausible, but they aren't obviously the same product yet. Treat the four capture ports (VOICE/CLIPBOARD/COMPILER_ALERT/OLLAMA_STREAM) and the terminal-routing goal as one candidate front end for the durable event spine described here, not as the definition of the product.

**Monitor count is a runtime fact, not a design assumption.** The target hardware ranges from 0 monitors (headless, or a machine driven purely by voice in/audio out with no display attached) up to 10. That rules out `RouteTarget.LEFT_TERMINAL` / `CENTER_TERMINAL` / `RIGHT_TERMINAL` as fixed enum values — a three-slot routing target can't express "route this to whichever of 7 monitors is free" or "there is no monitor, fall back to audio only." The Spatial Routing Engine needs:

- routing targets addressed by role or by a discovered display ID, not by a fixed left/center/right enum;
- a query for how many displays currently exist, so routing rules can degrade gracefully as that number changes (a laptop undocking from 3 monitors to 0, a session moving from a workstation to a headless SSH box);
- audio/sonification and the Markdown transcript projection treated as always-available projections, independent of display count, since they're the only presentation channel guaranteed to exist at 0 monitors;
- routing rules that are data (or at least configuration), not compiled-in cases, since "3 monitors" was never a safe constant to begin with.