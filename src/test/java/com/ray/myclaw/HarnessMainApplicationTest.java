package com.ray.myclaw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
                )
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

    private static int occurrencesOf(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) >= 0) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
