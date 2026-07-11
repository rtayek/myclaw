# Handoff: Building a Small Java AI Command-Line Harness

## Goal

Explore how to build a small, understandable AI harness, simpler than OpenClaw, probably in Java. The goal is not to build a new foundation model. The goal is to run existing AI tools and models from the command line, pipe prompts and responses back and forth, and gradually add structure.

A better framing:

> Build a small OpenClaw-like command-line wrapper in Java.

## Motivation

OpenClaw is powerful but complicated. It involves agents, rooms, plugins, gateway, dashboard, Telegram/Discord, cron, auth, model providers, config, security, and state. That complexity is useful eventually, but it is overkill for understanding the core mechanism.

The user wants to understand the simpler core:

```text
Java process
  → starts or calls AI backend
  → sends prompt/input
  → reads response/output
  → saves transcript/summary
  → maybe updates Markdown/JSON knowledge files
```

## Current Working AI Backends

### Claude CLI

Claude Code works from WSL. The command:

```sh
claude -p "Say exactly: PRINT_MODE_OK"
```

returned:

```text
PRINT_MODE_OK
```

OpenClaw can use Claude through the CLI model path:

```text
claude-cli/claude-sonnet-4-6
```

In the OpenClaw dashboard, this works:

```text
/model claude-cli/claude-sonnet-4-6
```

### OpenAI / GPT-5.5 Through OpenClaw

OpenClaw’s default model should remain:

```text
openai/gpt-5.5
```

This is the stable default for OpenClaw work.

### Ollama / Qwen

Ollama/Qwen works directly, but was slow or fragile inside the full OpenClaw agent runtime. Use it directly for now, not as the primary OpenClaw backend.

Useful direct pattern:

```sh
ollama run qwen-openclaw-small
```

or for API-style calls:

```text
http://127.0.0.1:11434
```

There is a WSL2 warning: Ollama autostart with CUDA and `Restart=always` may create WSL crash-loop risk. Keep Ollama manual unless needed.

## Desired Java Harness Concepts

Start very small.

### Phase 1: Run a One-Shot Command

Java runs:

```sh
claude -p "some prompt"
```

and captures stdout/stderr/exit code.

Java class idea:

```text
CommandRunner
  run(command, args, input, timeout)
  returns:
    exitCode
    stdout
    stderr
    duration
```

This should be backend-agnostic.

### Phase 2: Define a Model Adapter Interface

Possible Java interface:

```java
interface AiBackend {
    AiResponse ask(AiRequest request) throws AiBackendException;
}
```

Possible implementations:

```text
ClaudeCliBackend
  calls claude -p

OllamaHttpBackend
  calls Ollama HTTP API

OpenAiHttpBackend
  calls OpenAI API later, if desired

OpenClawCliBackend
  calls openclaw agent/chat commands
```

### Phase 3: Save Transcripts

Each run should produce a durable record:

```text
runs/
  2026-07-10T18-30-00-claude.md
```

Containing:

```md
# AI Run

Backend: Claude CLI
Command: claude -p
Time: ...
Exit code: ...

## Prompt

...

## Response

...

## stderr

...
```

### Phase 4: Add Sessions

A session is a sequence of turns:

```text
Session
  id
  backend
  messages
  created
  updated
```

Initially, sessions can just be Markdown or JSON files.

### Phase 5: Add Knowledge Extraction

Later, use an LLM to extract durable knowledge:

```text
raw transcript → summary.md → nodes.json + edges.json
```

This connects to the broader durable-knowledge idea:

```text
Markdown for humans.
JSON/YAML/SQLite edges for machines.
Git for time/history.
LLMs for extraction.
Graph/index layer for relationships.
```

## Important Distinction

Do not start with rooms, agents, plugins, Discord, Telegram, or cron.

Start with this:

```text
Java
  ProcessBuilder
    claude -p prompt
  capture stdout
  save Markdown
```

Then expand only after that works.

## Initial Java Design Sketch

Core objects:

```text
AiRequest
  prompt
  systemPrompt optional
  timeout
  metadata

AiResponse
  text
  stderr
  exitCode
  backendName
  duration

AiBackend
  ask(AiRequest) -> AiResponse

CommandRunner
  low-level process execution

TranscriptWriter
  saves prompt/response to Markdown

HarnessMain
  command-line entry point
```

First command-line target:

```sh
java -jar ai-harness.jar claude "Say exactly: HELLO"
```

Expected behavior:

```text
prints response
writes transcript file
returns nonzero exit if backend fails
```

## Coding Preferences

The user prefers:

```text
Java
clear names
tests as functional specification
minimal comments
package-private by default unless public is required
paste-safe shell commands
simple Gradle/Eclipse project layout
```

Avoid overly abstract architecture at the start. Prefer a small working vertical slice.

## First Implementation Goal

Create a Java Gradle project that can:

1. Run `claude -p <prompt>`.
2. Capture stdout/stderr/exit code.
3. Print the response.
4. Save a Markdown transcript.
5. Have unit tests for command execution using harmless commands.
6. Have an integration/smoke test for Claude only if explicitly enabled.

## Key Question for New Chat

What is the smallest Java program that can safely run:

```sh
claude -p "Say exactly: OK"
```

capture the output, and save a transcript?

Start there.
