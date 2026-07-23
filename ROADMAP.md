# Roadmap

The organizing goal is the **first milestone**: the developer runs MyClaw
daily, any response can be read aloud with one keystroke, and yesterday's
conversation can be reopened today. Work that does not serve that is
deferred.

## Phase 1: Core Workbench & Backends (current)

- [x] Local 3-tier desktop runtime (Java UI, Core Broker, Backend Engine).
- [x] Socket transport on `127.0.0.1` TCP loopback, protocol v1
      (`health`, `listBackends`, `chat`).
- [x] Local Ollama (`glm4:9b`) and Claude CLI backends.
- [x] Append-only dual logging: `.jsonl` telemetry and `.md` transcripts.
- [x] High-contrast accessible Swing UI with screen reader compatibility.
- [x] `SessionRuntime` recording immutable session events around
      `PromptService`.
- [ ] Derive Markdown solely from session events, removing the duplicate
      write path.

## Phase 2: Accessibility & Interaction (in progress)

- [ ] Speech-to-text dictation and text-to-speech output.
- [ ] Read the last response aloud with a single keystroke.
- [ ] Tonal sonification of build, test, and background state events.
- [ ] Global hotkey text-buffer capture from the editor.
- [ ] Predictable output regions; correct behavior on a single screen.
- [ ] Offline fallback: if the network drops or a cloud limit is hit, fall
      back to a local model and say so plainly.

## Phase 3: Memory, Skills & Library (next)

- [ ] Curated memory file, reread at session start and appendable.
- [ ] Local `SKILL.md` loading with progressive disclosure.
- [ ] Scheduled consolidation: a nightly job that reads session events and
      writes summaries, tags, and indexes alongside the record — the
      archive becoming more useful over time, not an agent acting unasked.
- [ ] Search across every conversation regardless of which backend
      answered, organized by project.
- [ ] Context handoff packets to continue a thread on a different model.
- [ ] Best-effort import of historical conversations, always distinguished
      from native capture, with originals preserved.

## Phase 4: Bounded Agent Loops (future)

Background execution is a capability under explicit user policy, not the
product's premise. Every policy states its bounds: maximum steps, elapsed
time, cost, permitted tools, permitted directories, approval requirements,
stop conditions, and whether unattended execution is allowed at all. The
journal records the policy in force and the decisions made under it.

- [ ] Tool execution from a small whitelist, each step requiring
      confirmation operable by keyboard and speech.
- [ ] Bounded scheduled task loops.
- [ ] Multi-model comparison, escalating selectively to cloud models as
      advisors.

## Not Planned

See the scope boundaries in `VISION.md`. In short: no public skill
registry, no cloud sandboxes or hosted multi-tenant service, no
prompt-to-hosted-app generation, no messaging-first reframing, no unbounded
autonomy.
