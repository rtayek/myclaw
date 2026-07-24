# Roadmap

The organizing milestone is daily use: the developer can run MyClaw as the
normal AI workbench, read a response aloud with one keystroke, and reopen
yesterday's conversation today.

## Phase 1: Daily-Use Desktop

Implemented and integrated:

- Accessible Swing desktop launched by `runDesktop`.
- In-process `PromptService` path from desktop to backend adapters.
- Claude CLI backend registered as `claude`.
- Ollama `glm4:9b` backend registered as `glm`.
- Command-line harness for direct backend prompts.
- Markdown transcript writes under `runs/`.
- Readable backend errors in the desktop path.

Implemented experimentally:

- Session event model, stores, and projections.
- SQLite-backed session event store.
- Socket transport with protocol v1 operations: `health`, `listBackends`, and
  `chat`.

Planned to complete the daily-use milestone:

- Reliable reopenable transcripts and basic session continuity in the
  desktop.
- Text-to-speech milestone: read the last response aloud with one keystroke.
- Speech-to-text dictation.
- Better keyboard-only navigation and focus behavior.
- Clearer offline and backend-unavailable states.

## Phase 2: Core and Backend Separation

Designed:

- Java desktop frontend remains the accessible product surface.
- Core owns sessions, context assembly, orchestration policy, approvals, and
  persistence coordination.
- Backend execution moves behind a provider-neutral local socket boundary.

Planned:

- Make the socket backend the normal desktop path.
- Add a backend process launcher and lifecycle management.
- Keep provider-neutral API messages separate from provider-specific command
  details.
- Add health, cancellation, and error propagation across the process boundary.
- Remove duplicate transcript paths and derive readable transcripts from
  session events.
- Preserve local-first operation while allowing explicitly selected cloud
  backends.

## Phase 3: Memory, Skills, and Library

Designed:

- Sessions outlive models and providers.
- Memory and derived artifacts remain separate from source records.
- Local skills use user-controlled files and progressive disclosure.

Planned:

- Curated project memory.
- Local `SKILL.md` loading.
- Search across conversations regardless of backend.
- Context handoff packets for continuing a thread with a different backend.
- Scheduled consolidation that summarizes, tags, and indexes stored sessions.
- Best-effort import of historical conversations, with originals preserved
  and imported records clearly distinguished from native capture.

## Phase 4: Policy-Governed Agent Work

Designed:

- Agent loops are policy-governed. Conservative limits are the default, while
  longer or unattended execution may be explicitly enabled.
- Policies specify maximum steps, elapsed time, cost or token budget, allowed
  tools, allowed directories, approval requirements, stop conditions, and
  unattended execution settings.

Planned:

- Tool adapters for a small, inspectable set of local actions.
- Coding loop: Claude Code or Codex edits files, Gradle runs tests, failures
  are inspected, revisions are made, and the user reviews the diff.
- Approval controls operable by keyboard and speech.
- Scheduled maintenance jobs under explicit policy.
- Optional unattended execution for selected policies.
- Multi-model review or comparison when it improves reliability.

## Not Planned

- Public skill marketplace.
- Hosted multi-tenant service.
- Prompt-to-hosted-app product.
- Cloud sandbox fleet maintained by this project.
