# Durable Session Runtime Notes

This pass keeps the existing prompt path intact. `SessionRuntime` wraps `PromptService.submit(...)` and records immutable session events before and after that call.

Reuse points:

* `PromptService` remains responsible for backend selection, request creation, command-backed execution, error normalization, and Markdown transcript writes.
* Existing `AiBackend`, `AiRequest`, `AiResponse`, prompt profiles, and transcript classes are unchanged.
* The new session store uses Gson, which is already a project dependency, for event payloads.
* Markdown output is temporarily duplicated as the existing PromptService side effect. A later pass can derive Markdown directly from session events once callers move to the runtime boundary.
