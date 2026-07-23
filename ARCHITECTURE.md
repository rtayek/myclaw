# System Architecture

## 3-Tier Model

MyClaw runs as a local, 3-tier desktop runtime:

```text
[ Java Desktop UI ] <---> [ Core Broker ] <---(Sockets)---> [ Backend Agent Engine ]
```

* **Java Desktop UI (frontend):** accessible Swing application — high
  contrast, screen reader compatible, keyboard-first navigation.
* **Core Broker:** event-driven Java core owning session state, prompt
  routing, thread management, and transcript persistence.
* **Backend Agent Engine:** execution layer (Ollama, Claude CLI) reached
  over loopback TCP sockets.

The client never needs to know whether a backend is a local process, a
socket, a local HTTP service, or a cloud API. That boundary lives in
`PromptService`, which owns backend selection, request creation, execution,
error normalization, and transcript writes.

## Socket Transport

Bound to `127.0.0.1` only, on a configurable port. Multiple connections are
supported; requests within one connection are strictly sequential — read a
frame, process it fully, write one terminal response, then read the next.
A client needing parallel work opens multiple connections.

**Framing:** newline-delimited UTF-8 JSON. One compact JSON object followed
by a single newline byte. JSON is never pretty-printed on the wire; embedded
line breaks are escaped as `\n` and never appear as literal framing
newlines. An optional trailing `\r` is accepted. Request frames are size-
capped (default 1 MiB) rather than read through an unbounded path.

**Protocol version 1 operations:** `health`, `listBackends`, `chat`.

```json
{"requestId":"1","operation":"health"}
{"requestId":"1","status":"ok","protocolVersion":1}

{"requestId":"3","operation":"chat","backendId":"ollama-qwen","prompt":"Explain recursion."}
```

An omitted `profile` defaults to `GENERAL`; `guided-teaching` is the other
value. Not in scope for v1: streaming, cancellation, pipelining, auth, TLS,
remote binding, conversation IDs, named pipes, Unix-domain sockets.

**Layering rule:** the transport adapter accepts connections, frames and
validates input, calls `PromptService`, and serializes responses. It must
not contain backend-specific behavior, command construction, prompt
construction, transcript policy, or any UI logic. It lives outside the
application and backend packages, in `myclaw.transport.socket`.

## Capture, Hash, Persist

Data moves through a thread-isolated pipeline so the UI never stalls:

```text
CaptureService -> HashService -> PersistenceService -> Routing
```

1. **`CaptureService`** — non-blocking NIO Reactor. A single `Selector`
   thread demultiplexes loopback capture ports: 7701 voice, 7702 clipboard,
   7703 compiler alerts, 7704 model stream. Producers can be as simple as
   `some-daemon | nc 127.0.0.1 7703`.
2. **`HashService`** — SHA-256 indexing of text blocks and error signatures.
   Repeated build errors and identical prompts are recognized and not
   re-run.
3. **`PersistenceService`** — dual-stream append-only journal: `.jsonl`
   transactional telemetry plus high-contrast `.md` transcripts kept
   readable for screen readers.
4. **Routing** — projects output streams to display regions and screen
   reader buffers.

The journal is authoritative. Transcripts, views, and audio are replayable
projections derived from it; derived artifacts never mutate the original
record.

## Session Runtime

`SessionRuntime` wraps `PromptService.submit(...)` and records immutable
session events before and after the call, without disturbing the existing
prompt path. `AiBackend`, `AiRequest`, `AiResponse`, prompt profiles, and
transcript classes are unchanged. Event payloads use Gson, already a project
dependency.

Markdown is currently written twice — once as a `PromptService` side effect,
once from session events. A later pass will derive Markdown solely from
session events once callers move to the runtime boundary.

## Modular Skills

Workflows are defined in `SKILL.md` files under `.agents/skills/`. Each is a
directory with YAML frontmatter (name, description, triggers) plus a
natural-language body.

Loading uses progressive disclosure: index only the frontmatter at startup
(~20 tokens per skill), match the incoming prompt against those
descriptions, and load the full body into context only on a match. Skill
injection attaches to `PromptService` during request assembly. The format is
portable, so skills written for other tools can be reused.

Local loading only — see the scope boundaries in `VISION.md` regarding
public registries.
