# HANDOFF: finish merging latency tests into myclaw

You are working in the myclaw repo (Java 21, Gradle, JUnit 6). The owner
started merging opt-in latency tests and did not finish. Complete it.

## Context

- The repo already has this pattern for opt-in tests: see the
  `claudeSmokeTest` and `ollamaSmokeTest` source sets, configurations,
  and tasks in build.gradle. The latency tests must follow it exactly.
- The test sources now live under `src/test/java/myclaw/`:
  ClaudeLatencyTest.java, OllamaLatencyTest.java, LatencyStats.java.
  They are tagged opt-in tests and should not run as part of ordinary `test`.

## Tasks

1. Ensure `src/test/java/myclaw/` contains the three latency files.

2. Edit build.gradle so ordinary `test` excludes the `latency` tag and
   register a task alongside claudeSmokeTest:

      tasks.register('latencyTest', Test) {
          description = 'Times backend round trips and alarms if too slow.'
          group = 'verification'
          testClassesDirs = sourceSets.test.output.classesDirs
          classpath = sourceSets.test.runtimeClasspath
          useJUnitPlatform { includeTags 'latency' }
          testLogging { showStandardStreams = true }
      }

3. Verify compilation through the normal test source set: `./gradlew testClasses`.
   Fix any compile errors (the tests use ClaudeCliBackend, OllamaCliBackend,
   CommandRunner, AiRequest from src/main - adjust to actual constructor
   signatures if they changed).

4. Confirm `./gradlew test` still passes (the ordinary suite must be
   unaffected).

5. Run `./gradlew latencyTest` ONCE and show the owner the timing tables
   it prints. Note: this spawns the real `claude` CLI and Ollama, so it
   is slow and costs a few API calls. That is expected. If Ollama or its
   model is unavailable, report that and continue - a failed
   OllamaLatencyTest for that reason is fine for now.

6. Do NOT commit. Show a summary of changes and let the owner review.

## Notes

- latency-history.csv will appear in the project root after a run; that
  is intentional (trend history). Suggest adding it to .gitignore.
- The owner has low vision: keep your final summary short and plainly
  formatted.
