# myclaw

Small Java command-line AI harness for one-shot prompts through Claude CLI or a local Ollama model.

## Build

```sh
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

## Test

```sh
./gradlew test
```

## Smoke Tests

These are separate from ordinary tests.

```sh
./gradlew claudeSmokeTest
```

```sh
./gradlew ollamaSmokeTest
```

`claudeSmokeTest` requires `claude` on PATH. `ollamaSmokeTest` requires Ollama on PATH and the `glm4:9b` model already installed.

## Run

```sh
java -jar .gradle-build/libs/myclaw.jar claude "Say exactly: OK"
```

Expected output:

```text
OK
```

Run the local Ollama-backed GLM alias:

```sh
java -jar .gradle-build/libs/myclaw.jar glm "Say exactly: GLM_OK"
```

Expected output includes:

```text
GLM_OK
```

Read the prompt from standard input by passing `-`:

```sh
printf '%s\n' 'Say exactly: CLAUDE_PIPE_OK' | ./gradlew run --args='claude -'
```

```sh
printf '%s\n' 'Say exactly: GLM_PIPE_OK' | ./gradlew run --args='glm -'
```

The `glm` backend runs locally through:

```text
ollama run glm4:9b
```

Prompt text is sent to Ollama over standard input. It does not call a hosted API. The model must already be installed.

## Desktop

Launch the Swing desktop client:

```sh
./gradlew runDesktop
```

Or from the shell launcher:

```sh
./scripts/myclaw-desktop
```

The desktop client provides:

```text
Backend selector: Claude or GLM
Multiline prompt input
Ctrl+Enter to send
Scrollable, copyable prompt/response display
Clear button for the on-screen display only
```

Each desktop request is still a one-shot backend call. The visible display can accumulate turns, but previous turns are not sent back as conversation history yet. Durable Markdown transcripts are still written under `runs/`.

Voice input is handled by the operating system. On Windows, focus the prompt area and use `Win+H`; the recognized text is sent as ordinary prompt text.

## Transcripts

Each invocation writes one Markdown transcript under `runs/`:

```text
runs/
  20260711T210000.000Z-550e8400-claude-cli.md
```

The transcript includes the prompt, response, command arguments, exit status, timeout status, duration, and stderr.

## Platform Note

The Java harness is platform-neutral, but `claude` and `ollama` must be available on the PATH of the environment that runs the jar.

Verified user environment for `glm4:9b`: Windows, 48 GB system RAM, NVIDIA RTX 4060 Ti with 8 GB VRAM, `glm4:9b` loaded 100% on GPU, active Ollama context 4096.
