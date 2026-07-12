package myclaw;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Times the full cycle for the Claude CLI backend: process spawn (startup)
 * plus round trip. Each ask() spawns a fresh `claude -p`, so startup cost
 * is included in every measurement - that is the current architecture.
 *
 * Run with: ./gradlew latencyTest
 * Requires `claude` on PATH. Results print to the test log.
 */
final class ClaudeLatencyTest {

    private static final int WARMUP_RUNS = 1;
    private static final int TIMED_RUNS = 5;
    // Alarm threshold: fail the test if average exceeds this.
    // Tune after your first few runs establish a baseline.
    private static final Duration MAX_AVG = Duration.ofSeconds(15);

    @Test
    void coldRoundTripStaysUnderThreshold() {
        ClaudeCliBackend backend = new ClaudeCliBackend(new CommandRunner(), Duration.ofSeconds(60));
        LatencyStats stats = LatencyStats.measure(
                "claude cold (spawn + round trip)",
                WARMUP_RUNS,
                TIMED_RUNS,
                () -> backend.ask(AiRequest.of("Reply with exactly: OK")));

        System.out.println(stats.report());
        assertTrue(stats.averageMillis() < MAX_AVG.toMillis(),
                "Average " + stats.averageMillis() + " ms exceeded " + MAX_AVG.toMillis() + " ms\n" + stats.report());
    }
}
