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
import java.util.List;
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
        HarnessMainApplication application = applicationWithBackends(
                Map.of("claude", backend),
                stdout,
                capturedStderr,
                "unused"
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

    @Test
    void applicationSelectsGlmBackendAndWritesOllamaTranscript() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(
                new CommandResult(0, "GLM_OK\n", "", Duration.ofMillis(25), false));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        HarnessMainApplication application = applicationWithBackends(
                Map.of("glm", new OllamaCliBackend(executor, Duration.ofMinutes(2), "glm4:9b")),
                stdout,
                new ByteArrayOutputStream(),
                "unused"
        );

        int exitCode = application.run(new String[]{"glm", "Say exactly: GLM_OK"});

        assertEquals(0, exitCode);
        assertEquals("GLM_OK\n", stdout.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("ollama", "run", "glm4:9b"), executor.request.command());
        assertEquals("Say exactly: GLM_OK", executor.request.standardInput());

        String transcript = latestTranscript();
        assertTrue(transcript.contains("Backend: Ollama glm4:9b"));
        assertTrue(transcript.contains("""
                ## Command

                ```
                ollama
                run
                glm4:9b
                ```
                """));
    }

    @Test
    void applicationStillSelectsClaudeBackend() {
        CapturingExecutor claudeExecutor = new CapturingExecutor(
                new CommandResult(0, "CLAUDE_OK\n", "", Duration.ofMillis(10), false));
        CapturingExecutor glmExecutor = new CapturingExecutor(
                new CommandResult(0, "GLM_OK\n", "", Duration.ofMillis(10), false));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        HarnessMainApplication application = applicationWithBackends(
                Map.of(
                        "claude", new ClaudeCliBackend(claudeExecutor, Duration.ofSeconds(5)),
                        "glm", new OllamaCliBackend(glmExecutor, Duration.ofMinutes(2), "glm4:9b")
                ),
                stdout,
                new ByteArrayOutputStream(),
                "unused"
        );

        int exitCode = application.run(new String[]{"claude", "Say exactly: CLAUDE_OK"});

        assertEquals(0, exitCode);
        assertEquals("CLAUDE_OK\n", stdout.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("claude", "-p", "Say exactly: CLAUDE_OK"), claudeExecutor.request.command());
        assertNull(glmExecutor.request);
    }

    @Test
    void unknownBackendReturnsUsageExitStatus() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        HarnessMainApplication application = applicationWithBackends(
                Map.of("glm", new OllamaCliBackend(request -> {
                    throw new AssertionError("backend should not run");
                }, Duration.ofMinutes(2), "glm4:9b")),
                new ByteArrayOutputStream(),
                stderr,
                "unused"
        );

        int exitCode = application.run(new String[]{"missing", "hello"});

        assertEquals(2, exitCode);
        assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void failedGlmTranscriptPreservesAttemptedCommandAndDoesNotDuplicateStderr() throws Exception {
        String stderr = "ollama error";
        CapturingExecutor executor = new CapturingExecutor(
                new CommandResult(2, "", stderr, Duration.ofMillis(30), false));
        ByteArrayOutputStream capturedStderr = new ByteArrayOutputStream();
        HarnessMainApplication application = applicationWithBackends(
                Map.of("glm", new OllamaCliBackend(executor, Duration.ofMinutes(2), "glm4:9b")),
                new ByteArrayOutputStream(),
                capturedStderr,
                "unused"
        );

        int exitCode = application.run(new String[]{"glm", "Say exactly: GLM_FAIL"});

        assertEquals(1, exitCode);
        assertEquals(1, occurrencesOf(capturedStderr.toString(StandardCharsets.UTF_8), stderr));

        String transcript = latestTranscript();
        assertTrue(transcript.contains("Error: Ollama glm4:9b exited with status 2: " + stderr));
        assertTrue(transcript.contains("""
                ## Command

                ```
                ollama
                run
                glm4:9b
                ```
                """));
    }

    @Test
    void timedOutGlmTranscriptPreservesAttemptedCommand() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(
                new CommandResult(-1, "partial", "", Duration.ofMinutes(2), true));
        HarnessMainApplication application = applicationWithBackends(
                Map.of("glm", new OllamaCliBackend(executor, Duration.ofMinutes(2), "glm4:9b")),
                new ByteArrayOutputStream(),
                new ByteArrayOutputStream(),
                "unused"
        );

        int exitCode = application.run(new String[]{"glm", "timeout prompt"});

        assertEquals(1, exitCode);
        String transcript = latestTranscript();
        assertTrue(transcript.contains("Timed out: true"));
        assertTrue(transcript.contains("ollama\nrun\nglm4:9b"));
    }

    @Test
    void startupFailedGlmTranscriptPreservesAttemptedCommand() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(
                new CommandExecutionException("Could not start command ollama"));
        HarnessMainApplication application = applicationWithBackends(
                Map.of("glm", new OllamaCliBackend(executor, Duration.ofMinutes(2), "glm4:9b")),
                new ByteArrayOutputStream(),
                new ByteArrayOutputStream(),
                "unused"
        );

        int exitCode = application.run(new String[]{"glm", "startup failure prompt"});

        assertEquals(1, exitCode);
        String transcript = latestTranscript();
        assertTrue(transcript.contains("Could not start Ollama glm4:9b"));
        assertTrue(transcript.contains("ollama\nrun\nglm4:9b"));
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
                fixedTranscriptWriter(),
                fixedClock(),
                new ResultReporter(
                        new PrintStream(backend.stdout, true, StandardCharsets.UTF_8),
                        new PrintStream(backend.stderr, true, StandardCharsets.UTF_8)
                ),
                new ByteArrayInputStream(standardInput.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void assertLatestTranscriptContains(String expected) throws Exception {
        assertTrue(latestTranscript().contains(expected));
    }

    private String latestTranscript() throws Exception {
        Path transcriptPath = Files.list(tempDir).findFirst().orElseThrow();
        return Files.readString(transcriptPath);
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

    private static final class CapturingExecutor implements CommandExecutor {
        private final CommandResult result;
        private final CommandExecutionException failure;
        private CommandRequest request;

        private CapturingExecutor(CommandResult result) {
            this.result = result;
            this.failure = null;
        }

        private CapturingExecutor(CommandExecutionException failure) {
            this.result = null;
            this.failure = failure;
        }

        @Override
        public CommandResult run(CommandRequest request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private HarnessMainApplication applicationWithBackends(
            Map<String, AiBackend> backends,
            ByteArrayOutputStream stdout,
            ByteArrayOutputStream stderr,
            String standardInput
    ) {
        return new HarnessMainApplication(
                new PromptService(backends, fixedTranscriptWriter(), fixedClock()),
                new ResultReporter(
                        new PrintStream(stdout, true, StandardCharsets.UTF_8),
                        new PrintStream(stderr, true, StandardCharsets.UTF_8)
                ),
                new ByteArrayInputStream(standardInput.getBytes(StandardCharsets.UTF_8))
        );
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
            this.application = new com.ray.myclaw.HarnessMainApplication(
                    new PromptService(backends, transcriptWriter, clock), reporter, input);
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

    private TranscriptWriter fixedTranscriptWriter() {
        return new TranscriptWriter(tempDir, fixedClock());
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC);
    }
}
