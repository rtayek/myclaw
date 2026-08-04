package myclaw.application;

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

import myclaw.backend.*;
import myclaw.execution.CommandResult;
import myclaw.transcript.TranscriptWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TranscriptIngestionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void ingestReadsMarkdownFileSubmitsPromptAndWritesConsolidatedFile() throws Exception {
        Path inputPath = tempDir.resolve("myproject-chat.md");
        Files.writeString(inputPath, "# Project Chat\nUser: Add feature X\nAssistant: Done adding feature X.");

        CapturingCommandBackend backend = new CapturingCommandBackend(
                new CommandBackedRun(
                        new AiResponse("# Summary\nFeature X was added.", new BackendId("Claude CLI"), Duration.ofMillis(10)),
                        new CommandResult(0, "OK", "", Duration.ofMillis(10), false),
                        List.of("claude", "-p", "prompt")
                )
        );
        PromptService promptService = serviceWith("claude", backend);
        TranscriptIngestionService ingestionService = new TranscriptIngestionService(promptService);

        Path outputPath = ingestionService.ingest(inputPath);

        Path expectedOutputPath = tempDir.resolve("myproject-chat_CONSOLIDATED.md");
        assertEquals(expectedOutputPath, outputPath);
        assertTrue(Files.exists(outputPath));
        assertEquals("# Summary\nFeature X was added.", Files.readString(outputPath));

        assertTrue(backend.capturedRequest.prompt().contains("User: Add feature X"));
        assertTrue(backend.capturedRequest.prompt().contains("Assistant: Done adding feature X."));
    }

    @Test
    void ingestReadsJsonFileAndExtractsMessages() throws Exception {
        Path inputPath = tempDir.resolve("myproject.json");
        String jsonContent = """
                [
                    {"role": "user", "content": "Implement feature Y"},
                    {"role": "assistant", "content": "Feature Y implemented successfully."}
                ]
                """;
        Files.writeString(inputPath, jsonContent);

        CapturingCommandBackend backend = new CapturingCommandBackend(
                new CommandBackedRun(
                        new AiResponse("Summary of Y", new BackendId("Claude CLI"), Duration.ofMillis(10)),
                        new CommandResult(0, "OK", "", Duration.ofMillis(10), false),
                        List.of("claude", "-p", "prompt")
                )
        );
        PromptService promptService = serviceWith("claude", backend);
        TranscriptIngestionService ingestionService = new TranscriptIngestionService(promptService);

        Path outputPath = ingestionService.ingest(inputPath, "claude");

        assertEquals(tempDir.resolve("myproject_CONSOLIDATED.md"), outputPath);
        assertTrue(Files.exists(outputPath));
        assertEquals("Summary of Y", Files.readString(outputPath));

        assertTrue(backend.capturedRequest.prompt().contains("USER: Implement feature Y"));
        assertTrue(backend.capturedRequest.prompt().contains("ASSISTANT: Feature Y implemented successfully."));
    }

    @Test
    void ingestNonExistentFileThrowsIllegalArgumentException() {
        PromptService promptService = serviceWith("claude", new CapturingCommandBackend((CommandBackedRun) null));
        TranscriptIngestionService ingestionService = new TranscriptIngestionService(promptService);

        assertThrows(IllegalArgumentException.class, () -> ingestionService.ingest(tempDir.resolve("missing.md")));
    }

    @Test
    void computeOutputPathDerivesConsolidatedFilename() {
        assertEquals(Path.of("/foo/bar/project_CONSOLIDATED.md"), TranscriptIngestionService.computeOutputPath(Path.of("/foo/bar/project.md")));
        assertEquals(Path.of("chat_CONSOLIDATED.md"), TranscriptIngestionService.computeOutputPath(Path.of("chat.json")));
        assertEquals(Path.of("test_CONSOLIDATED.md"), TranscriptIngestionService.computeOutputPath(Path.of("test")));
    }

    @Test
    void extractMessagesHandlesJsonAndPlainMarkdown() {
        Path mdPath = Path.of("sample.md");
        String mdContent = "Just raw markdown lines";
        assertEquals("Just raw markdown lines", TranscriptIngestionService.extractMessages(mdPath, mdContent));

        Path jsonPath = Path.of("sample.json");
        String jsonContent = "{\"messages\": [{\"role\": \"user\", \"content\": \"Hi\"}]}";
        assertEquals("USER: Hi", TranscriptIngestionService.extractMessages(jsonPath, jsonContent));
    }

    private PromptService serviceWith(String name, AiBackend backend) {
        Clock clock = Clock.fixed(Instant.parse("2026-08-03T20:00:00Z"), ZoneOffset.UTC);
        return new PromptService(Map.of(name, backend), new TranscriptWriter(tempDir, clock), clock);
    }

    private static final class CapturingCommandBackend implements CommandBackedAiBackend {
        private final CommandBackedRun result;
        private AiRequest capturedRequest;

        private CapturingCommandBackend(CommandBackedRun result) {
            this.result = result;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            return askWithResult(request).response();
        }

        @Override
        public CommandBackedRun askWithResult(AiRequest request) {
            this.capturedRequest = request;
            return result;
        }

        @Override
        public List<String> commandFor(AiRequest request) {
            return List.of("claude", "-p", request.prompt());
        }
    }
}
