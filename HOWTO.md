# HOWTO and Quick Start

## Environment

The project is a Java desktop application built with Gradle. The Gradle
toolchain is configured for Java 21.

## Build and Test

```sh
./gradlew build
./gradlew test
./gradlew integrationTest
```

Backend smoke and latency tasks also exist:

```sh
./gradlew claudeSmokeTest
./gradlew ollamaSmokeTest
./gradlew latencyTest
```

The smoke and latency tasks depend on local external tools such as `claude`
or `ollama` being installed and configured.

## Running MyClaw

### Desktop Application

```sh
./gradlew runDesktop
```

### Command Line

The command-line harness accepts a backend ID and a prompt. Current backend
IDs are `claude` and `glm`.

```sh
java -jar .gradle-build/libs/myclaw.jar claude "Say exactly: OK"
java -jar .gradle-build/libs/myclaw.jar glm "Say exactly: GLM_OK"
```

Piped prompt:

```sh
printf '%s\n' 'Say exactly: OK' | ./gradlew run --args='claude -'
```

## Socket Transport

The socket transport is implemented and covered by integration tests, but it
is not yet the normal desktop execution path. There is no Gradle task that
starts a packaged backend socket process.

When started from code, `MyClawSocketServer` binds to `127.0.0.1` using a
`SocketServerConfig` port. It speaks newline-delimited compact JSON:

```sh
printf '%s\n' '{"requestId":"1","operation":"health"}' | nc 127.0.0.1 <port>
printf '%s\n' '{"requestId":"2","operation":"listBackends"}' | nc 127.0.0.1 <port>
printf '%s\n' '{"requestId":"3","operation":"chat","backendId":"glm","prompt":"Explain recursion."}' | nc 127.0.0.1 <port>
```

See `ARCHITECTURE.md` for current and proposed protocol details.

## Git Workflow

Use commit message files rather than inline `-m`, which is paste-safe and
avoids quoting problems in Git Bash:

```sh
cat > commit-message.txt <<'EOF'
Commit title

Detailed description of changes.
EOF
git add -A
git commit -F commit-message.txt
rm commit-message.txt
```

## Documentation Convention

Root documentation is exactly five authoritative files: `README.md`,
`VISION.md`, `ARCHITECTURE.md`, `HOWTO.md`, and `ROADMAP.md`.

Superseded drafts live in `old-mds/` and `old/`; they are historical and
should not be treated as current.

When working with an LLM on this project, ask it to edit these five files
rather than generate new root-level design documents.
