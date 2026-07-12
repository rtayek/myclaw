package com.ray.myclaw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarnessMainApplicationTest {
    @TempDir
    Path tempDir;

    @Test
    void failedClaudeTranscriptPreservesAttemptedCommand() throws Exception {
        String prompt = "Say \"FAILED\"; $HOME `test`";
        String stderr = "authentication required";
        CommandResult result = new CommandResult(1, "", stderr, Duration.ofMillis(15), false);
        ClaudeCliBackend backend = new ClaudeCliBackend(request -> result, Duration.ofSeconds(5));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedStderr = new ByteArrayOutputStream();
        HarnessMainApplication application = new HarnessMainApplication(
                Map.of("claude", backend),
                new TranscriptWriter(tempDir, Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC)),
                Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC),
                new ResultReporter(
                        new PrintStream(stdout, true, StandardCharsets.UTF_8),
                        new PrintStream(capturedStderr, true, StandardCharsets.UTF_8)
                ),
                new ByteArrayInputStream("unused".getBytes(StandardCharsets.UTF_8))
        );

        int exitCode = application.run(new String[]{"claude", prompt});

        assertEquals(1, exitCode);
        assertEquals("", stdout.toString(StandardCharsets.UTF_8));
        String stderrOutput = capturedStderr.toString(StandardCharsets.UTF_8);
        assertTrue(stderrOutput.contains("Claude CLI exited with status 1: " + stderr));
        assertEquals(1, occurrencesOf(stderrOutput, stderr));

        Path transcriptPath = Files.list(tempDir).findFirst().orElseThrow();
        String transcript = Files.readString(transcriptPath);
        assertTrue(transcript.contains("Error: Claude CLI exited with status 1: " + stderr));
        assertTrue(transcript.contains("""
                ## Command

                ```
                claude
                -p
                Say "FAILED"; $HOME `test`
                ```
                """));
        assertEquals(1, occurrencesOf(transcript, "claude\n-p\n" + prompt));
    }

    @Test
    void literalArgumentIsPassedUnchangedAndDoesNotConsumeInput() throws Exception {
        CapturingBackend backend = new CapturingBackend(new AiResponse("ARGUMENT_OK\n", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, "stdin should remain unused");

        int exitCode = application.run(new String[]{"fake", "Say exactly: ARGUMENT_OK"});

        assertEquals(0, exitCode);
        assertEquals("Say exactly: ARGUMENT_OK", backend.request.prompt());
        assertLatestTranscriptContains("Say exactly: ARGUMENT_OK");
    }

    @Test
    void dashReadsPromptFromStandardInput() throws Exception {
        CapturingBackend backend = new CapturingBackend(new AiResponse("PIPE_OK\n", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, "Say exactly: PIPE_OK");

        int exitCode = application.run(new String[]{"fake", "-"});

        assertEquals(0, exitCode);
        assertEquals("Say exactly: PIPE_OK", backend.request.prompt());
        assertEquals("PIPE_OK\n", application.stdout());
        assertLatestTranscriptContains("Say exactly: PIPE_OK");
    }

    @Test
    void multilineStandardInputIsPreservedExactly() {
        String prompt = "Review this:\n\nline one\nline two";
        CapturingBackend backend = new CapturingBackend(new AiResponse("done\n", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, prompt);

        int exitCode = application.run(new String[]{"fake", "-"});

        assertEquals(0, exitCode);
        assertEquals(prompt, backend.request.prompt());
    }

    @Test
    void unicodeStandardInputIsPreservedExactly() {
        String prompt = "Explain: café, π, 日本語";
        CapturingBackend backend = new CapturingBackend(new AiResponse("done\n", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, prompt);

        int exitCode = application.run(new String[]{"fake", "-"});

        assertEquals(0, exitCode);
        assertEquals(prompt, backend.request.prompt());
    }

    @Test
    void shellLikeStandardInputRemainsData() {
        String prompt = "\"quotes\"; $HOME `command`";
        CapturingBackend backend = new CapturingBackend(new AiResponse("done\n", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, prompt);

        int exitCode = application.run(new String[]{"fake", "-"});

        assertEquals(0, exitCode);
        assertEquals(prompt, backend.request.prompt());
    }

    @Test
    void trailingNewlineFromStandardInputIsPreserved() throws Exception {
        String prompt = "Say exactly: PIPE_OK\n";
        CapturingBackend backend = new CapturingBackend(new AiResponse("done\n", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, prompt);

        int exitCode = application.run(new String[]{"fake", "-"});

        assertEquals(0, exitCode);
        assertEquals(prompt, backend.request.prompt());
        assertLatestTranscriptContains("Say exactly: PIPE_OK\n");
    }

    @Test
    void emptyStandardInputIsRejectedWithoutCallingBackend() throws Exception {
        CapturingBackend backend = new CapturingBackend(new AiResponse("unused", new BackendId("Fake"), Duration.ofMillis(5)));
        TestApplication application = applicationWith(backend, "");

        int exitCode = application.run(new String[]{"fake", "-"});

        assertEquals(2, exitCode);
        assertNull(backend.request);
        assertTrue(application.stderr().contains("Prompt from standard input is empty."));
        assertEquals(0, Files.list(tempDir).count());
    }

    private static int occurrencesOf(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) >= 0) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    private TestApplication applicationWith(CapturingBackend backend, String standardInput) {
        return new TestApplication(
                Map.of("fake", backend),
                new TranscriptWriter(tempDir, Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC)),
                Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC),
                new ResultReporter(
                        new PrintStream(backend.stdout, true, StandardCharsets.UTF_8),
                        new PrintStream(backend.stderr, true, StandardCharsets.UTF_8)
                ),
                new ByteArrayInputStream(standardInput.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void assertLatestTranscriptContains(String expected) throws Exception {
        Path transcriptPath = Files.list(tempDir).findFirst().orElseThrow();
        assertTrue(Files.readString(transcriptPath).contains(expected));
    }

    private static final class CapturingBackend implements AiBackend {
        private final AiResponse response;
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        private AiRequest request;

        private CapturingBackend(AiResponse response) {
            this.response = response;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            this.request = request;
            return response;
        }
    }

    private static final class TestApplication {
        private final com.ray.myclaw.HarnessMainApplication application;
        private final CapturingBackend backend;

        private TestApplication(
                Map<String, AiBackend> backends,
                TranscriptWriter transcriptWriter,
                Clock clock,
                ResultReporter reporter,
                ByteArrayInputStream input
        ) {
            this.backend = (CapturingBackend) backends.values().iterator().next();
            this.application = new com.ray.myclaw.HarnessMainApplication(backends, transcriptWriter, clock, reporter, input);
        }

        private int run(String[] args) {
            return application.run(args);
        }

        private String stdout() {
            return backend.stdout.toString(StandardCharsets.UTF_8);
        }

        private String stderr() {
            return backend.stderr.toString(StandardCharsets.UTF_8);
        }
    }
}
