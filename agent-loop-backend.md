# MyClaw Backend Addendum: Context and Agent Loops

## Purpose

This addendum extends `openai-backend.md` with four optional capabilities:

- local skills,
- curated memory,
- scheduled tasks,
- agent loops using external tools.

They should build on the existing session, event, journal, and projection model rather than creating a separate agent platform.

## Policy

MyClaw should work well for a user who remains present, but presence is not mandatory.

Agent loops should be bounded by default, but the bounds are policy rather than an architectural prohibition. The user may explicitly permit longer or unattended execution.

Every execution policy should state, where applicable:

- maximum steps,
- maximum elapsed time,
- token or cost limits,
- permitted tools,
- permitted files or working directories,
- approval requirements,
- stop conditions,
- whether unattended execution is allowed.

The journal should record the selected policy and the important decisions made under it.

## Context Assembly

Memory and skills are readable source artifacts used to assemble request context.

```text
Prompt request
    + selected memory
    + selected skills
    + relevant session context
    = backend request
```

Suggested abstractions:

```java
interface ContextContributor {
    ContextFragment contribute(ContextRequest request);
}

final class ContextAssembler {
    AssembledContext assemble(ContextRequest request);
}
```

Possible contributors include:

- `SharedMemoryContributor`,
- `ProjectMemoryContributor`,
- `ToolMemoryContributor`,
- `SelectedSkillContributor`,
- `RecentSessionContributor`.

The journal should record which memory and skill files were used, including their paths, hashes, or versions. The reusable files remain in their libraries; the backend records usage rather than copying the same file into every session.

## Skills

A skill is a local reusable instruction package, initially represented by a `SKILL.md` file.

The first implementation should support:

1. a searchable local skill library,
2. manual skill selection,
3. loading the selected skill into request context,
4. recording the selected skill and its content hash,
5. no automatic permission merely because a skill requests an action.

A skill may instruct an LLM to search the web, inspect files, or use another capability, but the execution policy and available tool adapters determine whether that request is allowed.

Automatic intent matching and progressive loading may be added later. They are optimizations, not prerequisites.

## Memory

Memory should begin as ordinary Markdown files:

```text
memory/
  shared.md
  projects/
    myclaw.md
  tools/
    claude.md
    codex.md
    ollama.md
```

Keep these concepts separate:

- journal: immutable record of what happened,
- session handoff: compact state needed to resume work,
- curated memory: stable decisions, preferences, constraints, and conceptual state.

LLMs may propose memory changes, but curated memory should remain visible and editable. Automatic consolidation may write to a pending file or create a proposed change rather than silently rewriting permanent memory.

## Scheduled Tasks

Scheduled work is an internal producer of commands or events, not necessarily a capture adapter.

```text
Scheduler
    → SCHEDULE_TRIGGERED event
    → prompt or task coordinator
    → backend or tool invocation
    → journaled result
```

Schedules should survive restart and expose:

- enabled state,
- next run,
- last run,
- execution history,
- overlap policy,
- selected model and cost policy,
- unattended-execution policy.

Scheduled summaries are a sensible first use.

## Agent Loops

MyClaw coordinates and records an agent loop. A selected external tool performs specialized work.

The first experiment should be a coding loop:

```text
Claude Code or Codex edits files
    → Gradle runs tests
    → test results return to the coding tool
    → tool revises the files
    → policy decides whether to continue
    → Ray reviews the final diff
```

MyClaw should own:

- loop state,
- execution policy,
- tool invocation,
- event capture,
- file-change and diff records,
- test-result capture,
- continuation decisions,
- pause, stop, and approval handling where configured,
- final presentation and replay.

The coding tool owns the actual editing.

Suggested concepts:

```java
record AgentLoopPolicy(
        OptionalInt maximumSteps,
        Optional<Duration> maximumDuration,
        OptionalLong maximumCostMicros,
        Set<ToolId> permittedTools,
        Set<Path> permittedRoots,
        ApprovalPolicy approvalPolicy,
        boolean unattendedAllowed) {
}

interface ToolAdapter {
    ToolDescriptor descriptor();
    ToolInvocation invoke(ToolRequest request, ToolEventSink sink);
    void cancel(ToolInvocation invocation);
}

interface LoopController {
    LoopRun start(LoopRequest request, AgentLoopPolicy policy);
    void pause(LoopRunId id);
    void resume(LoopRunId id);
    void stop(LoopRunId id);
}
```

Limits may be absent or deliberately relaxed when the user chooses a broader policy. The important requirement is that the policy is explicit and the execution remains reconstructable afterward.

## Journal Events

Useful event kinds include:

```text
CONTEXT_ASSEMBLED
MEMORY_LOADED
SKILL_LOADED
SCHEDULE_TRIGGERED
LOOP_STARTED
TOOL_INVOCATION_STARTED
TOOL_OUTPUT_RECEIVED
FILES_CHANGED
TESTS_COMPLETED
APPROVAL_REQUESTED
APPROVAL_RECORDED
LOOP_CONTINUED
LOOP_PAUSED
LOOP_STOPPED
LOOP_COMPLETED
LOOP_FAILED
```

These events should make supervised and unattended runs equally inspectable.

## Recommended Implementation Order

1. Add `ContextAssembler` and read-only Markdown memory.
2. Load one manually selected local skill.
3. Record memory and skill hashes in session events.
4. Add persisted scheduled summaries.
5. Add one coding `ToolAdapter` and Gradle test invocation.
6. Implement a small loop controller with a conservative default policy.
7. Add pause, stop, approval, and unattended policy options.
8. Evaluate broader loops only after the first workflow is reliable.

## Boundary

These capabilities do not require MyClaw to become a hosted cloud service, public skill registry, Lovable-style application generator, or universal autonomous-agent platform.

They extend the existing mission:

> MyClaw is an accessible, local-first cockpit and durable continuity layer that may supervise or run AI-assisted work under policies chosen by the user.
