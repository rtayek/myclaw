package myclaw.backend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import myclaw.execution.CommandRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Times the Ollama CLI backend. The warmup run loads the model into RAM,
 * so the timed runs measure warm cycles (CLI spawn + inference on a
 * resident model). If run 1 of the warmups is huge, that was model load.
 *
 * Run with: ./gradlew latencyTest
 * Requires Ollama on PATH and the model installed.
 */
final class OllamaLatencyTest {

    private static final String MODEL = "glm4:9b";   // matches ApplicationBackends
    private static final int WARMUP_RUNS = 1;
    private static final int TIMED_RUNS = 5;
    private static final Duration MAX_AVG = Duration.ofSeconds(30);

    @Tag("latency")
    @Test
    void warmRoundTripStaysUnderThreshold() {
        OllamaCliBackend backend = new OllamaCliBackend(new CommandRunner(), Duration.ofMinutes(2), MODEL);
        LatencyStats stats = LatencyStats.measure(
                "ollama " + MODEL + " warm (spawn + inference)",
                WARMUP_RUNS,
                TIMED_RUNS,
                () -> backend.ask(AiRequest.of("Reply with exactly: OK")));

        System.out.println(stats.report());
        assertTrue(stats.averageMillis() < MAX_AVG.toMillis(),
                "Average " + stats.averageMillis() + " ms exceeded " + MAX_AVG.toMillis() + " ms\n" + stats.report());
    }
}
