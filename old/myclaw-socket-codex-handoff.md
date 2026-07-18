# MyClaw Socket Transport Implementation Handoff for Codex

## Purpose

Implement the first localhost socket transport for MyClaw.

The guided-teaching slice is already implemented and tested:

- `PromptProfile` contains `GENERAL` and `GUIDED_TEACHING`.
- `AiRequest` carries the profile.
- Existing two-argument `AiRequest` construction remains supported.
- `PromptService.submit(backendName, prompt)` retains general behavior.
- `PromptService.submit(backendName, prompt, profile)` supports guided teaching.
- Claude and Ollama CLI backends use `request.effectivePrompt()`.
- `AiRequest.prompt()` preserves the learner's original text.
- Existing unit and integration tests pass.

Do not redesign that slice as part of this work.

The goal of this increment is:

> A localhost client sends one compact JSON request line, the socket adapter translates it into an existing `PromptService` call, and the server returns one compact JSON response line.

---

## Existing Architectural Boundary

```text
Socket client
    ↓
socket transport adapter
    ↓
PromptService
    ↓
AiBackend
```

The socket adapter may:

1. accept TCP connections;
2. read and frame UTF-8 JSON lines;
3. parse and validate external input;
4. translate wire values to application values;
5. call `PromptService`;
6. translate results and exceptions into wire responses;
7. serialize compact JSON response lines.

The socket adapter must not contain:

- Ollama-specific behavior;
- Claude-specific behavior;
- command construction;
- teaching prompt construction;
- transcript policy;
- model-provider selection rules beyond passing `backendId`;
- HTTP, servlet, Swing, JavaFX, or browser logic.

Keep transport code outside `myclaw.application` and `myclaw.backend`. Choose a package consistent with the repository, such as:

```text
myclaw.transport.socket
```

Do not reorganize unrelated packages or folders.

---

## Required Protocol Version 1 Scope

Implement only:

```text
health
listBackends
chat
```

Do not implement streaming, cancellation, pipelining, authentication, TLS, remote binding, conversation IDs, graceful shutdown draining, named pipes, or Unix-domain sockets.

---

## Binding and Connection Model

- Bind only to `127.0.0.1`.
- Make the port configurable.
- Support multiple client connections.
- Each connection may carry multiple requests.
- Requests within one connection are strictly sequential:
  1. read one frame;
  2. process it fully;
  3. write one terminal response;
  4. read the next frame.
- Do not process multiple in-flight requests on one connection.
- A client requiring parallel work may open multiple connections.

Prefer the simplest concurrency mechanism already consistent with the project and Java version. Avoid speculative thread-pool infrastructure.

---

## Framing Rules

Use newline-delimited UTF-8 JSON.

A frame is:

> One compactly serialized JSON object followed by a single newline byte.

Requirements:

- Do not pretty-print JSON.
- Embedded line breaks in prompt or response strings must be JSON-escaped as `\n`; they must not appear as literal framing newlines.
- Emit `\n`.
- Accept an optional trailing `\r` before the terminating `\n`.
- Enforce a maximum request-frame size.
- Use a named constant or configuration value.
- A default maximum of 1 MiB is reasonable unless the current project has a better configuration pattern.
- Do not use an unbounded `BufferedReader.readLine()` path that can allocate arbitrarily large input.

Test multiline prompt and response strings explicitly.

---

## Wire Requests

### Health

```json
{"requestId":"1","operation":"health"}
```

### List backends

```json
{"requestId":"2","operation":"listBackends"}
```

### General chat

```json
{"requestId":"3","operation":"chat","backendId":"ollama-qwen","prompt":"Explain recursion."}
```

### Guided teaching

```json
{"requestId":"4","operation":"chat","backendId":"ollama-qwen","profile":"guided-teaching","prompt":"Help me understand fractions."}
```

The omitted profile defaults to `GENERAL`.

---

## Wire Responses

### Health success

```json
{"requestId":"1","status":"ok","protocolVersion":1}
```

### Backend-list success

```json
{
  "requestId":"2",
  "status":"ok",
  "backends":[
    {"id":"claude","label":"Claude CLI"},
    {"id":"ollama-qwen","label":"Qwen through Ollama"}
  ]
}
```

Compact serialization is required on the wire even though examples are formatted for readability.

Expose only an opaque backend ID and human-readable label. Do not expose command paths, transport type, executable names, process details, credentials, or provider internals.

### Chat success

```json
{"requestId":"4","status":"ok","backendId":"ollama-qwen","content":"..."}
```

### Error

```json
{
  "requestId":"4",
  "status":"error",
  "error":{
    "code":"unknown_backend",
    "message":"No backend is registered with id 'missing'."
  }
}
```

For malformed JSON where no request ID can be recovered:

```json
{
  "requestId":null,
  "status":"error",
  "error":{
    "code":"malformed_json",
    "message":"The request is not valid JSON."
  }
}
```

Do not expose stack traces, command lines, credentials, filesystem locations, or raw exception details.

---

## Validation Rules

For every request:

- top-level JSON value must be an object;
- `requestId` is required;
- `requestId` must be a nonblank string;
- `operation` is required;
- `operation` must be a nonblank recognized string;
- unknown fields should be ignored for forward-compatible extension;
- the frame must not exceed the configured maximum.

For `health`:

- no operation-specific fields are required.

For `listBackends`:

- no operation-specific fields are required.

For `chat`:

- `backendId` is required and must be a nonblank string;
- `prompt` is required and must be a nonblank string;
- `profile` is optional;
- missing `profile` means `GENERAL`;
- accepted external profile names:
  - `general`
  - `guided-teaching`
- an unknown profile produces `validation_error`.

Keep external-name translation in the socket adapter unless the existing project already has a clear convention for transport names on enums.

---

## Error Codes

Use this closed version 1 set:

```text
malformed_json
validation_error
unknown_operation
unknown_backend
backend_unavailable
backend_failed
unsupported_request
internal_error
request_too_large
```

Map existing application/backend failures rather than inventing new business rules.

Use the actual exception hierarchy in the repository. The intended mappings are:

```text
unknown backend selection
    → unknown_backend

AiBackendStartupException
    → backend_unavailable

AiBackendExecutionException
    → backend_failed

AiBackendUnsupportedRequestException
    → unsupported_request

unexpected unchecked exception
    → internal_error
```

For `internal_error`:

- log the original failure server-side;
- send only a generic client message.

For syntactically malformed JSON:

- send `malformed_json` if possible;
- then close the connection.

For valid JSON that fails field validation or names an unknown operation:

- send an error response;
- keep the connection open.

For an oversized frame:

- send `request_too_large` if safely possible;
- then close the connection.

---

## Timeout and Disconnect Rules

Keep two concerns separate:

### Socket idle-read timeout

A connection that sends no complete request for the configured idle period may be closed.

Use project-consistent configuration. Do not choose a tiny timeout that makes interactive use unreliable.

### Backend execution timeout

Do not create a second arbitrary model-generation timeout in the socket adapter.

Backend execution remains governed by existing backend behavior and configuration. Translate backend timeout/failure exceptions into the appropriate protocol error.

### Client disconnect during backend work

Version 1 does not provide cancellation.

If a client disconnects while `PromptService` is processing:

- the backend operation may continue;
- the server discards the result when the response cannot be written;
- do not build cancellation machinery in this increment.

---

## Backend Listing

Inspect the current `ApplicationBackends`, `BackendId`, `BackendChoice`, registry, or equivalent types.

Reuse the existing source of backend IDs and labels. Do not duplicate backend configuration in the socket package.

If `PromptService` currently does not expose backend descriptors, add only the narrowest application-facing query needed by the transport. Do not allow the socket adapter to reach into provider-specific implementation details.

Explain any required API change before or alongside implementing it.

---

## JSON Library

Inspect existing dependencies first.

- Reuse an existing JSON library if one is already present.
- If none exists, choose the smallest conventional library that fits the current Java/Gradle project.
- Do not write a hand-rolled general JSON parser.
- Do not add a web framework merely to parse JSON.

Report any dependency added and why.

---

## Required Tests

Use the repository's existing source sets, frameworks, naming style, and fake-backend conventions.

At minimum, test:

### Framing

- compact request followed by newline;
- multiline prompt survives framing and parsing;
- multiline response is escaped and returned as one frame;
- CRLF input is accepted;
- oversized frame is rejected;
- malformed JSON returns `malformed_json` and closes the connection.

### Validation

- missing request ID;
- blank request ID;
- missing operation;
- unknown operation;
- missing chat backend ID;
- missing chat prompt;
- blank chat prompt;
- unknown profile;
- unknown fields are ignored.

### Operations

- health returns status and protocol version;
- listBackends returns ID and label only;
- general chat calls `PromptService` with `GENERAL`;
- omitted profile defaults to `GENERAL`;
- guided chat calls `PromptService` with `GUIDED_TEACHING`;
- response echoes request ID;
- chat response echoes backend ID.

### Errors

- unknown backend mapping;
- startup failure mapping;
- execution failure mapping;
- unsupported request mapping;
- unexpected exception becomes generic `internal_error`.

### Connection behavior

- two sequential requests work over one connection;
- a valid request still works after a valid-JSON validation failure;
- one connection does not process two requests concurrently.

Use a fake backend through the real `PromptService`. Do not launch a real Ollama or Claude process merely to test socket framing.

---

## Coding Preferences

- Tests are the functional specification.
- Code is the primary documentation.
- Prefer meaningful names over comments.
- Use package-private visibility unless wider visibility is required.
- Avoid speculative interfaces and inheritance.
- Keep changes small and coherent.
- Preserve existing public behavior.
- Do not perform broad refactoring.
- Do not commit unless explicitly asked by the user.

---

## Expected Deliverable

Provide:

1. repository inspection relevant to the socket seam;
2. selected package structure;
3. JSON dependency decision;
4. implementation summary;
5. validation and exception mapping;
6. changed files;
7. commands run;
8. all test results;
9. known limitations;
10. recommendation for the next increment;
11. whether the work was committed.

---

## Definition of Done

The increment is done when:

- a server binds only to localhost;
- health, backend listing, and chat work over one-line JSON frames;
- general and guided-teaching chat both use the existing `PromptService`;
- multiple sequential requests work on one connection;
- request sizes are bounded;
- errors are structured and sanitized;
- multiline text is framed correctly;
- tests use a fake backend rather than real AI processes;
- no socket concern leaks into backend implementations;
- no HTTP, servlet, Swing, JavaFX, streaming, or cancellation feature is added.
