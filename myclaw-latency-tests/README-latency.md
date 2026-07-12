# Latency tests for myclaw

Timing tests that follow your existing smoke-test pattern: their own
source set, opt-in, run only when you ask.

## Install

1. Copy `src/latencyTest/` into the repo next to `src/claudeSmokeTest/`.

2. Add to build.gradle - inside the existing `sourceSets { }` block:

```groovy
    latencyTest {
        java.srcDir 'src/latencyTest/java'
        compileClasspath += sourceSets.main.output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
```

   Inside the existing `configurations { }` block:

```groovy
    latencyTestImplementation.extendsFrom testImplementation
    latencyTestRuntimeOnly.extendsFrom testRuntimeOnly
```

   And a new task alongside claudeSmokeTest:

```groovy
tasks.register('latencyTest', Test) {
    description = 'Times backend round trips and alarms if too slow.'
    group = 'verification'
    testClassesDirs = sourceSets.latencyTest.output.classesDirs
    classpath = sourceSets.latencyTest.runtimeClasspath
    useJUnitPlatform()
    testLogging { showStandardStreams = true }   // print the timing tables
}
```

## Run

```sh
./gradlew latencyTest
```

## What it does

- ClaudeLatencyTest: 1 warmup + 5 timed asks through ClaudeCliBackend.
  Every ask spawns a fresh `claude -p`, so each number is the full cycle
  you feel today: startup + round trip.
- OllamaLatencyTest: same shape; the warmup loads the model into RAM so
  timed runs are warm cycles.
- Both print a min/avg/max table and FAIL if the average exceeds a
  threshold (15 s claude, 30 s ollama - loose on purpose; tighten them
  once you know your baseline).
- Each run appends a line to latency-history.csv in the project root,
  so over weeks you get a trend file: did an update make things slower?

## Later

When the persistent-pipe backend exists, add a third test that opens the
pipe once and times warm turns. Comparing its numbers to
ClaudeLatencyTest in the same file quantifies exactly what the pipe buys.
