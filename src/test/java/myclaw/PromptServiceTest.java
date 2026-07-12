package myclaw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PromptServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void submitWritesTranscriptAndReturnsPromptResult() throws Exception {
        CapturingCommandBackend backend = new CapturingCommandBackend(
                new CommandBackedRun(
                        new AiResponse("OK\n", new BackendId("Fake CLI"), Duration.ofMillis(8)),
                        new CommandResult(0, "OK\n", "", Duration.ofMillis(8), false),
                        List.of("fake", "run")
                )
        );
        PromptService service = serviceWith("fake", backend);

        PromptResult result = service.submit("fake", "Say exactly: OK");

        assertEquals("Fake CLI", result.backendLabel());
        assertEquals("OK\n", result.response());
        assertTrue(Files.exists(result.transcriptPath()));
        assertEquals("Say exactly: OK", backend.request.prompt());
        assertTrue(Files.readString(result.transcriptPath()).contains("fake\nrun"));
    }

    @Test
    void failedSubmitWritesTranscriptBeforeRethrowing() throws Exception {
        AiBackendException failure = new AiBackendExecutionException(
                "Fake CLI exited with status 9",
                new BackendId("Fake CLI"),
                new CommandResult(9, "", "bad", Duration.ofMillis(8), false)
        );
        CapturingCommandBackend backend = new CapturingCommandBackend(failure);
        PromptService service = serviceWith("fake", backend);

        assertThrows(AiBackendException.class, () -> service.submit("fake", "fail"));

        String transcript = Files.readString(Files.list(tempDir).findFirst().orElseThrow());
        assertTrue(transcript.contains("Error: Fake CLI exited with status 9"));
        assertTrue(transcript.contains("fake\nrun"));
    }

    private PromptService serviceWith(String name, AiBackend backend) {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC);
        return new PromptService(Map.of(name, backend), new TranscriptWriter(tempDir, clock), clock);
    }

    private static final class CapturingCommandBackend implements CommandBackedAiBackend {
        private final CommandBackedRun result;
        private final AiBackendException failure;
        private AiRequest request;

        private CapturingCommandBackend(CommandBackedRun result) {
            this.result = result;
            this.failure = null;
        }

        private CapturingCommandBackend(AiBackendException failure) {
            this.result = null;
            this.failure = failure;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            return askWithResult(request).response();
        }

        @Override
        public CommandBackedRun askWithResult(AiRequest request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return result;
        }

        @Override
        public List<String> commandFor(AiRequest request) {
            return List.of("fake", "run");
        }
    }
}
