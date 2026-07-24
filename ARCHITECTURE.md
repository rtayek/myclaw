# System Architecture

## Current Implementation

The desktop currently runs the prompt path in one JVM. `DesktopMain`
constructs `PromptService`, `ApplicationBackends.create()` registers the
available backend adapters, and the Swing frame submits directly through that
service.

```text
DesktopMain
    |
PromptService
    |
AiBackend implementations
```

Current integrated pieces:

- `src/myclaw/desktop/DesktopMain.java` launches the Swing desktop.
- `src/myclaw/application/PromptService.java` owns backend lookup, request
  creation, backend execution, error normalization, and transcript writes.
- `src/myclaw/application/ApplicationBackends.java` registers backend IDs
  `claude` and `glm`.
- `src/myclaw/backend/*` contains the Claude CLI and Ollama command-backed
  backend adapters.
- `src/myclaw/session/*` contains session event types, in-memory and SQLite
  stores, and projection code.
- `src/myclaw/transport/socket/*` contains an implemented socket transport
  with integration tests, but the desktop does not yet use it as its normal
  execution path.

The current desktop writes Markdown transcripts through `PromptService`.
Session event storage exists, but Markdown transcript generation has not yet
been fully consolidated around replayable session events.

## Target Process Architecture

The intended architecture separates presentation, application state, and
backend execution:

```text
Java Desktop Frontend
        |
       Core
        |
     sockets
        |
 Backend Process
```

Responsibilities:

- **Frontend:** accessible Java desktop presentation and input, including
  keyboard flow, screen-reader friendly text, high-contrast views, speech, and
  predictable focus behavior.
- **Core:** application state, sessions, context assembly, backend selection,
  orchestration policy, approvals, and persistence coordination.
- **Socket transport:** provider-neutral process boundary between Core and
  backend execution.
- **Backend process:** model calls, tool execution, scheduled jobs, and agent
  loops.
- **Persistence:** source records, transcripts, replay, indexes, summaries,
  and derived artifacts.

The target process split is not yet the desktop's integrated path. Related
classes and tests should be treated as implementation progress, not evidence
that the full runtime separation is complete.

## Socket Protocol

Implemented transport details:

- Binds to `127.0.0.1`.
- Port is supplied through `SocketServerConfig`; tests commonly use port `0`
  for an ephemeral port.
- Frames are newline-delimited compact UTF-8 JSON objects.
- Request frames are size-capped; the default maximum is 1 MiB.
- Multiple client connections are accepted.
- Requests on one connection are processed sequentially.
- Protocol version 1 operations are `health`, `listBackends`, and `chat`.
- `chat` accepts `backendId`, `prompt`, and optional `profile`.
- Supported profiles are the default general profile and `guided-teaching`.

Example frames:

```json
{"requestId":"1","operation":"health"}
{"requestId":"2","operation":"listBackends"}
{"requestId":"3","operation":"chat","backendId":"glm","prompt":"Explain recursion."}
```

Experimental or incomplete:

- Running the socket server as the desktop's normal backend path.
- A packaged backend process launcher.
- Health and error propagation across the full desktop/core/backend boundary.

Proposed later operations and capabilities:

- Streaming responses.
- Cancellation.
- Conversation and session identifiers in protocol messages.
- Tool execution.
- Scheduled jobs.
- Agent-loop control messages.
- Stronger process lifecycle management.

Not currently in scope for the local socket protocol: remote binding by
default, TLS as a loopback requirement, or exposing MyClaw as a hosted public
service.

## Sessions and Persistence

Sessions are the durable product unit. A session should preserve enough source
record to explain what happened, replay useful views, and move between models
or providers without losing context.

The target persistence model separates source records from projections:

- Source records: prompts, responses, selected backend, policy decisions,
  tool calls, failures, timestamps, and artifact references.
- Transcripts: readable Markdown derived from the source record.
- Replay: reconstructable session views from stored events.
- Derived artifacts: summaries, tags, indexes, and handoff packets that never
  silently rewrite the source record.

Current code includes session event stores and projections, while the
integrated prompt path still writes Markdown transcripts directly. A future
cleanup should remove duplicate transcript paths and derive readable
transcripts from session events.

## Skills, Memory, Scheduling, and Agent Loops

Skills, memory, scheduling, and agent loops are optional extensions to the
desktop workbench. They should remain inspectable and policy-governed rather
than becoming a hidden autonomous layer.

Local skills should be loaded from user-controlled files, such as `SKILL.md`,
with progressive disclosure: index concise metadata first, then load detailed
instructions only when relevant.

Memory should be curated, local, and tied to sessions and projects. Scheduled
work should record what policy authorized it, what it touched, and what it
produced.

The first coding loop should be simple and review-centered:

```text
Claude Code or Codex edits files
    |
Gradle runs tests
    |
agent inspects failures
    |
agent revises
    |
user reviews the diff
```

Execution policy should support:

- Maximum steps.
- Elapsed time.
- Cost or token budget.
- Allowed tools.
- Allowed directories.
- Approval requirements.
- Stop conditions.
- Unattended execution setting.

Conservative limits should be defaults. Longer runs, broader tool access, or
unattended execution should require explicit user-selected policy.
