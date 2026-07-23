# MyClaw HOWTO & Quick Start

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

### Command-Line Interface (CLI)
```sh
# Direct JAR execution
java -jar .gradle-build/libs/myclaw.jar claude "Say exactly: OK"
java -jar .gradle-build/libs/myclaw.jar glm "Say exactly: GLM_OK"

# Piped prompt via Gradle
printf '%s\n' 'Say exactly: OK' | ./gradlew run --args='claude -'
```

## Developer Guidelines & Git Workflow
* **Environment:** Windows 11, Java 25, Gradle, Eclipse, Git Bash.
* **Paste-Safe Commit Style:** Use commit message files instead of inline `-m` flags:
  ```sh
  cat > commit-message.txt <<'EOF'
  Commit title

  Detailed description of changes.
  EOF
  git add -A
  git commit -F commit-message.txt
  rm commit-message.txt
  ```
