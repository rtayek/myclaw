# HOWTO & Quick Start

## Environment

Windows 11, Java 25, Gradle, Eclipse, Git Bash.

## Build and Test

```sh
./gradlew build
./gradlew test
./gradlew integrationTest
```

### Backend Smoke Tests

```sh
./gradlew claudeSmokeTest
./gradlew ollamaSmokeTest
./gradlew latencyTest
```

## Running MyClaw

### Desktop Application

```sh
./gradlew runDesktop
```

### Command Line

```sh
# Direct JAR execution
java -jar .gradle-build/libs/myclaw.jar claude "Say exactly: OK"
java -jar .gradle-build/libs/myclaw.jar glm "Say exactly: GLM_OK"

# Piped prompt via Gradle
printf '%s\n' 'Say exactly: OK' | ./gradlew run --args='claude -'
```

### Socket Transport

The transport binds to `127.0.0.1` on a configurable port and speaks
newline-delimited compact JSON. A quick health check:

```sh
printf '%s\n' '{"requestId":"1","operation":"health"}' | nc 127.0.0.1 <port>
```

Capture ports accept plain text from any producer:

```sh
some-daemon | nc 127.0.0.1 7703   # compiler alerts
```

See `ARCHITECTURE.md` for the full protocol.

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

Root documentation is exactly five files: `README.md`, `VISION.md`,
`ARCHITECTURE.md`, `HOWTO.md`, and `ROADMAP.md`.

Superseded drafts live in `old-mds/` and `old/`; they are historical and
should not be treated as current.

When working with an LLM on this project, ask it to **edit these five
files** rather than generate new ones. The pile in `old-mds/` is what
happens otherwise — a dozen competing vision statements, several of which
described a different product.
