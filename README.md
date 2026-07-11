# myclaw

Small Java command-line AI harness for one-shot Claude CLI prompts.

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

## Claude Smoke Test

This is separate from ordinary tests and requires `claude` to be available.

```sh
./gradlew claudeSmokeTest
```

## Run

```sh
java -jar .gradle-build/libs/myclaw-0.1.0.jar claude "Say exactly: OK"
```

Expected output:

```text
OK
```

## Transcripts

Each invocation writes one Markdown transcript under `runs/`:

```text
runs/
  20260711T210000.000Z-550e8400-claude-cli.md
```

The transcript includes the prompt, response, command arguments, exit status, timeout status, duration, and stderr.

## Platform Note

The Java harness is platform-neutral, but `claude` must be available on the PATH of the environment that runs the jar. Claude has been verified from WSL; running the jar from Windows PowerShell will only work if `claude` is also available there.
