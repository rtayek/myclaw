# MyClaw

MyClaw is an accessible, local-first Java desktop workbench for using
interchangeable AI backends while keeping conversations and artifacts in
durable user-owned records.

## What Exists Now

The desktop is a Swing application that runs in one JVM. It constructs
`PromptService` directly and uses in-process backend adapters for Claude CLI
and Ollama `glm4:9b`.

```text
Java Desktop
    |
PromptService
    |
in-process AiBackend implementations
```

The current application can run from Gradle, submit prompts to the registered
backends, and write readable Markdown transcripts under `runs/`.

## Intended Direction

The target architecture separates the accessible Java desktop from backend
execution through a local socket boundary:

```text
Java Desktop Frontend
        |
       Core
        |
 local socket protocol
        |
 Backend Process
```

That boundary is the intended direction for model execution, tools,
scheduled work, and policy-governed agent loops. MyClaw prioritizes
accessible, inspectable interaction. It may also perform supervised or
unattended work under explicit user-selected policies.

## Authoritative Documentation

- [VISION.md](VISION.md) - purpose and principles.
- [ARCHITECTURE.md](ARCHITECTURE.md) - current implementation and target
  architecture.
- [HOWTO.md](HOWTO.md) - build, test, run, and workflow notes.
- [ROADMAP.md](ROADMAP.md) - implemented, experimental, designed, and planned
  work.
