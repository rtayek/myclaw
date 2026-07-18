# MyClaw Socket Protocol Review Handoff for Gemini

## Purpose

Review the proposed version 1 MyClaw socket protocol after the initial design discussion.

Do not implement code.

The goal is to find contradictions, missing cases, unnecessary complexity, compatibility hazards, and test gaps before Codex implements the transport.

MyClaw already has:

- `PromptService` as the AI-facing application service;
- `AiBackend` implementations;
- backend selection by ID;
- `PromptProfile.GENERAL`;
- `PromptProfile.GUIDED_TEACHING`;
- guided teaching translated into a provider-facing effective prompt;
- the learner's original prompt preserved separately;
- passing unit and integration tests.

The socket adapter will call the existing service. It must not implement provider logic or educational orchestration.

---

## Proposed Version 1 Transport

```text
TCP
127.0.0.1 only
UTF-8
newline-delimited compact JSON
multiple sequential requests per connection
one in-flight request per connection
```

A client wanting parallel work opens another connection.

The server emits `\n`. It accepts `\n` or `\r\n`.

Embedded newlines inside JSON strings must be escaped. No literal newline byte may occur before the frame terminator.

The maximum request frame will be bounded, probably at 1 MiB.

---

## Proposed Operations

```text
health
listBackends
chat
```

No streaming, cancellation, pipelining, authentication, TLS, conversation IDs, remote binding, named pipes, or Unix-domain sockets in version 1.

---

## Proposed Requests

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

Profile names:

```text
general
guided-teaching
```

An omitted profile defaults to `general`.

---

## Proposed Responses

### Health

```json
{"requestId":"1","status":"ok","protocolVersion":1}
```

### List backends

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

Wire serialization remains compact.

### Chat

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

If no request ID can be recovered:

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

---

## Proposed Error Codes

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

Expected exception mapping:

```text
unknown backend
    → unknown_backend

AiBackendStartupException
    → backend_unavailable

AiBackendExecutionException
    → backend_failed

AiBackendUnsupportedRequestException
    → unsupported_request

unexpected exception
    → internal_error
```

Internal errors are logged in detail but sent to the client with a generic message.

Malformed JSON and oversized frames close the connection after an error response if one can safely be written.

Valid JSON with invalid fields returns an error and keeps the connection open.

---

## Proposed Validation

Every request:

- top-level JSON value is an object;
- `requestId` is a required nonblank string;
- `operation` is a required nonblank recognized string;
- unknown fields are ignored;
- request frame size is bounded.

Chat:

- `backendId` is required and nonblank;
- `prompt` is required and nonblank;
- `profile` is optional;
- unknown profile produces `validation_error`.

---

## Proposed Timeouts and Disconnect Behavior

- A socket idle-read timeout may close abandoned connections.
- Backend processing uses the backend's existing timeout behavior.
- The socket adapter does not add a competing generation timeout.
- There is no cancellation in version 1.
- If the client disconnects during processing, the backend may continue and the result is discarded when it cannot be written.

---

## Questions to Review

Please critically analyze the proposal and answer:

1. Is newline-delimited compact JSON sufficiently specified to avoid framing ambiguity?
2. Is byte-counting versus character-counting addressed correctly for UTF-8 and a 1 MiB maximum?
3. What is the simplest safe bounded-frame reader design in Java?
4. Should a frame be rejected for literal carriage returns or other control characters not represented through JSON escaping?
5. Is closing after malformed JSON the right rule, given that a newline frame boundary is still known?
6. Is closing after an oversized line implementable without buffering the whole line?
7. Are unknown fields best ignored in version 1, or should they be rejected to expose client mistakes?
8. Should duplicate JSON object keys be rejected?
9. Should JSON numbers, arrays, booleans, or null values in string fields all produce the same `validation_error`?
10. Should blank prompts be rejected, or could whitespace have a valid future meaning?
11. Is `requestId: null` the best malformed-request response, or should the field be omitted?
12. Should request IDs have a maximum length?
13. Should backend IDs and profile names have maximum lengths?
14. Is the proposed error-code set coherent and stable?
15. Is `unknown_backend` an application error or a validation error?
16. Should error messages be stable protocol text, or human-oriented text that may change?
17. Does echoing `backendId` create any future ambiguity if routing or fallback is later added?
18. Is `protocolVersion` only in health sufficient, or should it appear elsewhere?
19. Should version 1 requests carry a version field from the beginning?
20. Is one request at a time per connection the right explicit rule?
21. Are there any deadlock or resource-leak risks when a client disconnects during a long backend call?
22. What server shutdown behavior is the minimum acceptable behavior for tests and local development?
23. Is a socket idle-read timeout needed in the first increment, or is it unnecessary complexity?
24. What Windows, WSL, Linux, CRLF, encoding, firewall, or localhost-binding problems should tests or documentation cover?
25. Is binding specifically to `127.0.0.1` preferable to `localhost`, and what are the IPv4/IPv6 implications?
26. Does `listBackends` need availability state, or should version 1 return only configured ID and label?
27. Can backend labels safely be considered presentation text?
28. Should `chat` return any existing `PromptResult` metadata besides content?
29. Does profile translation belong in the socket adapter or on `PromptProfile`?
30. Are any proposed version 1 features unnecessary and removable?
31. Is anything essential missing?
32. Which tests are most likely to catch subtle implementation defects?

---

## Requested Deliverable

Produce:

1. a concise verdict;
2. accepted decisions;
3. required changes before implementation;
4. optional improvements that may be deferred;
5. rejected or unnecessary ideas;
6. a corrected version 1 protocol if changes are needed;
7. a prioritized test matrix;
8. explicit Windows/WSL/Linux interoperability notes;
9. a list of future extensions that remain backward-compatible;
10. a list of future extensions that would require a protocol-version change.

Be skeptical, precise, and economical.

Do not propose HTTP, WebSocket, gRPC, a distributed system, or a multi-agent architecture as a replacement. The question is whether this small localhost socket protocol is internally sound and testable.
