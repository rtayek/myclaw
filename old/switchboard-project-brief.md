# Switchboard

## Mission

**Switchboard** is an accessible, multi-window desktop workbench for conducting, observing, preserving, finding, and organizing AI sessions across models, tools, and projects.

Its purpose is to give one user a single place to work with multiple local and cloud AI systems without being trapped inside separate command-line tools, browser tabs, vendor interfaces, or incompatible chat histories.

Switchboard should reduce the fragmentation caused by hundreds of chats spread across many AI services, command-line tools, projects, and interfaces. It should support both live AI sessions and previously saved or imported conversations through one consistent local environment.

## Problem

AI work is currently scattered across:

- browser-based AI products;
- local and cloud LLMs;
- command-line tools such as Claude CLI, Codex, Gemini, and Ollama;
- hundreds of separate chats;
- incompatible transcript and export formats;
- multiple projects and subjects;
- vendor-specific histories that are difficult to search, compare, preserve, or reorganize.

Useful reasoning, decisions, code, research, and project knowledge become trapped in transient conversations. The user must remember which AI, project, chat, or interface contains a particular result.

Switchboard exists to make those conversations visible, durable, searchable, and manageable.

## MVP Goal

The MVP should prove that one person can comfortably manage several AI sessions at once through one accessible desktop application.

The MVP should allow the user to:

1. Open multiple independent AI sessions.
2. Connect each session to an existing AI backend, local model, service, or command-line tool.
3. Display the complete exchange between the user and the backend.
4. Preserve each interaction in a consistent local transcript format.
5. Organize live and imported sessions into named projects or groups.
6. Search saved sessions by title and transcript text.
7. Restore session windows and arrange them across any number of monitors.
8. Import at least one existing chat format.
9. Reopen prior sessions without depending on the original provider interface.

The MVP coordinates sessions for the user. It does not yet autonomously coordinate teams of LLMs.

## Core Principle

The fundamental unit is a **session**, not a model.

A model is replaceable. A session is the durable record of work.

A session may include:

- selected backend and model;
- user messages;
- assistant responses;
- streaming output;
- standard output and standard error;
- timestamps;
- execution status;
- errors;
- tool or command activity;
- transcript location;
- project or group membership;
- window and view state.

The complete session should be preserved even when the user chooses to view only part of it.

## Capture and Presentation

Capture must be independent of presentation.

```text
Backend, CLI, local process, socket, or service
    -> session events
    -> transcript storage
    -> one or more views
```

The user interface should not depend directly on whether a backend uses process execution, sockets, HTTP, or another transport.

The transcript should remain useful even if the original backend is unavailable later.

## Session Views

A session may be displayed through one or more views.

### Conversation View

Shows the normal user and assistant exchange.

### Detailed View

Adds timestamps, backend identity, model identity, elapsed time, status changes, and errors.

### Raw View

Shows exact input, output, standard error, exit codes, protocol messages, and other low-level traffic.

The original record should remain unchanged. Views are projections of the stored session.

## Multi-Window Interface

Switchboard should support any number of independent windows rather than being designed for a fixed number of monitors.

A user with three monitors might arrange:

- the dashboard or session library on one monitor;
- a primary AI conversation on another;
- another AI, raw console, transcript, or status window on the third.

Windows should eventually be:

- movable;
- resizable;
- detachable;
- restorable;
- associated with sessions or projects;
- usable with large fonts and keyboard navigation.

Accessibility is a core requirement, not a cosmetic feature to be added later after everyone has already suffered.

## Dashboard and Organization

The dashboard should organize work without becoming a giant flat collection of icons.

Possible top-level groupings include:

- Projects
- AI Assistants
- Session Groups
- Recent Work
- Saved Chats
- Imported Chats
- Tools

A dashboard item may open:

- a live AI session;
- a project workspace;
- a saved transcript;
- a chat library;
- a raw console;
- a status or monitoring view.

Projects and groups should be more important than provider names. The backend is a worker inside a project, not necessarily the primary organizing principle.

## Chat Management

Switchboard should later absorb the useful ideas from ChatMap or Chat Manager.

Saved and imported sessions may eventually support:

- search;
- titles;
- tags;
- project grouping;
- topic grouping;
- links between related chats;
- summaries;
- semantic extraction;
- durable Markdown files;
- structured knowledge records;
- import and export across providers.

The system must distinguish original records from derived artifacts.

### Original Record

The exact captured or imported conversation.

### Derived Artifacts

Summaries, extracted decisions, project notes, indexes, embeddings, tags, and other interpretations.

Derived artifacts must never silently rewrite the original record.

## Existing Projects and Reuse

Several existing projects explore parts of Switchboard.

### TinyLLM

Explores models, training, inference, evaluation, and local LLM behavior.

Repository:

```text
https://github.com/rtayek/tinyllm.git
```

### MiniReader

Explores ingestion, extraction, storage, indexing, search, and summarization of external material.

Repository:

```text
https://github.com/rtayek/miniReader.git
```

### ChatMap / Chat Manager

Explores chat import, normalization, browsing, organization, and semantic extraction.

Repository:

```text
https://github.com/rtayek/chatmap.git
```

### OpenClaw

Explores agents, channels, tools, local execution, model access, and orchestration.

### MyClaw

Provides the strongest current foundation for the live-session workbench:

- Java desktop and CLI clients;
- `PromptService`;
- `AiBackend` abstraction;
- command execution;
- Claude CLI and Ollama backends;
- transcript persistence;
- prompt profiles;
- localhost socket transport;
- Gradle test structure.

Switchboard should evolve from MyClaw rather than beginning as another mostly overlapping repository.

## Existing-Code Reuse Policy

The existing repositories are not merely historical references. Reviewers and implementers should inspect them for reusable code, tests, data models, storage formats, importers, UI components, and architectural patterns.

For each repository, identify:

1. Code that can be reused unchanged.
2. Code that can be adapted behind a new interface.
3. Tests that can be preserved as functional specifications.
4. Data formats or migration logic worth retaining.
5. Architectural ideas worth copying without copying the implementation.
6. Code that should remain separate because it is experimental, obsolete, overly coupled, or written for a different runtime.

Do not merge repositories wholesale.

Prefer selective reuse through explicit interfaces and small, reviewable changes.

When proposing reuse, record:

- source repository and file;
- purpose of the reused code;
- dependencies introduced;
- required modifications;
- tests that prove equivalent behavior;
- licensing or attribution requirements, if any.

The goal is to preserve proven work without importing accidental complexity.

## Conceptual Model

```text
Switchboard
    Dashboard
    Session Library
    Project Workspaces

Project or Group
    Sessions
    Files
    Derived Knowledge
    Window Layout

Session
    Backend Connection
    Session Events
    Transcript
    Views

Backend
    CLI
    Local Process
    Socket
    HTTP Service
    Cloud API
```

Possible session events include:

```text
UserMessage
AssistantChunk
AssistantMessage
StatusChanged
StandardOutput
StandardError
ToolInvocation
CommandStarted
CommandCompleted
Failure
```

## MVP Scope

### Included

- at least two AI backends;
- multiple independent live sessions;
- multiple desktop windows;
- visible request and response flow;
- transcript persistence;
- session library;
- projects or named groups;
- basic transcript search;
- import of at least one existing chat format;
- restoration of saved sessions and window state;
- accessible fonts, zoom, keyboard use, and uncluttered presentation.

### Deferred

- autonomous multi-agent workflows;
- automatic task delegation;
- business-running agent teams;
- learner models;
- long-term autonomous memory;
- complex semantic knowledge graphs;
- unattended external actions;
- elaborate plugin systems;
- broad cloud synchronization;
- automatic restructuring of project knowledge.

These may become later layers, but they should not be allowed to consume the MVP merely because software enjoys pretending every first release is a civilization.

## First Demonstrable Milestone

The first meaningful milestone should prove the center of the product:

1. Launch Switchboard.
2. Open two independent session windows.
3. Connect them to two different AI backends.
4. Send prompts to both.
5. Observe conversation, status, errors, and raw traffic.
6. Save both transcripts locally.
7. Close and reopen the application.
8. Find both sessions in the library.
9. Restore the windows.
10. Search for text contained in one transcript.

## Later Team Capability

After the session and transcript foundations are reliable, sessions may be organized into AI teams.

Examples:

```text
Software Team
    Architect
    Programmer
    Test Reviewer
    Documentation Reviewer

Business Team
    Planner
    Researcher
    Accountant
    Critic
    Writer

Learning Team
    Tutor
    Problem Solver
    Proof Checker
    Exercise Generator
```

A role consists of:

- a backend or model;
- role instructions;
- permitted tools;
- shared workspace access;
- output destination;
- execution and cost limits.

The first team mode should remain manual. The user directs each session and explicitly moves information between them.

Automated coordination should come later and must include:

- visible inter-agent messages;
- step limits;
- token or cost limits;
- tool permissions;
- pause and cancel controls;
- approval gates;
- complete audit transcripts.

The future teaching environment can be implemented as a Switchboard workspace or team rather than defining the entire product.

For example:

```text
Switchboard
    School
    Software Lab
    Business Office
    Research Desk
```

## Design Constraints

- The user remains in control.
- Every AI exchange must be observable.
- Original transcripts must be preserved.
- Local storage should be the default.
- Provider-specific behavior must remain behind adapters.
- Projects and sessions should survive backend changes.
- Gradle command-line builds are authoritative.
- Eclipse should be supported as a plain Java project without Buildship.
- Accessibility is a core requirement.
- The architecture should remain useful with one model, several models, or no live model connection.
- Tests should serve as the functional specification.
- Code should communicate intent through meaningful names rather than explanatory comments.

## Repository Direction

The recommended path is:

1. Preserve the current MyClaw state with a tag.
2. Rename the existing MyClaw repository to Switchboard.
3. Update user-visible names and documentation first.
4. Delay broad Java package renaming until it provides clear value.
5. Add this document to the repository.
6. Inspect ChatMap, MiniReader, TinyLLM, and OpenClaw selectively for reusable code and patterns.
7. Build the first demonstrable milestone before adding autonomous coordination.

## Short Product Definition

> **Switchboard is a visual, accessible, multi-window workbench for conducting, preserving, finding, and organizing AI sessions across models, tools, and projects.**

## Review Questions

Reviewers should consider:

1. Is the MVP narrow enough to build without becoming a general agent platform?
2. Is `Session` the correct central abstraction?
3. What is the smallest durable transcript and event model?
4. Which parts of MyClaw should be retained unchanged?
5. Which code from ChatMap, MiniReader, and TinyLLM can be reused safely?
6. Which ideas should remain separate until later?
7. What is the simplest useful session library?
8. What single import format should be supported first?
9. How should raw CLI traffic and conversation messages coexist in one transcript?
10. What architectural choices would preserve a clean path toward future manual and automated AI teams?
