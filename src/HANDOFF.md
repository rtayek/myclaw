# HANDOFF: finish merging latency tests into myclaw

You are working in the myclaw repo (Java 21, Gradle, JUnit 6). The owner
started merging opt-in latency tests and did not finish. Complete it.

## Context

- The repo already has this pattern for opt-in tests: see the
  `claudeSmokeTest` and `ollamaSmokeTest` source sets, configurations,
  and tasks in build.gradle. The latency tests must follow it exactly.
- The test sources are in this zip under `src/latencyTest/java/myclaw/`:
  ClaudeLatencyTest.java, OllamaLatencyTest.java, LatencyStats.java.
  They may already be partially copied into the repo - check first,
  and prefer the versions from this zip if they differ.

## Tasks

1. Ensure `src/latencyTest/java/myclaw/` exists in the repo with
   the three files from this zip.

2. Edit build.gradle:
   a. Inside the existing `sourceSets { }` block add:

      latencyTest {
          java.srcDir 'src/latencyTest/java'
          compileClasspath += sourceSets.main.output + configurations.testRuntimeClasspath
          runtimeClasspath += output + compileClasspath
      }

   b. Inside the existing `configurations { }` block add:

      latencyTestImplementation.extendsFrom testImplementation
      latencyTestRuntimeOnly.extendsFrom testRuntimeOnly

   c. Register a task alongside claudeSmokeTest:

      tasks.register('latencyTest', Test) {
          description = 'Times backend round trips and alarms if too slow.'
          group = 'verification'
          testClassesDirs = sourceSets.latencyTest.output.classesDirs
          classpath = sourceSets.latencyTest.runtimeClasspath
          useJUnitPlatform()
          testLogging { showStandardStreams = true }
      }

3. Verify compilation only: `./gradlew compileLatencyTestJava`.
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
