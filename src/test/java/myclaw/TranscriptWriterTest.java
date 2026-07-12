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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TranscriptWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesOneMarkdownTranscriptPerRun() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC);
        TranscriptWriter writer = new TranscriptWriter(tempDir, clock);
        Transcript transcript = Transcript.success(
                "550e8400-e29b-41d4-a716-446655440000",
                new BackendId("Claude CLI"),
                clock.instant(),
                Duration.ofMillis(42),
                AiRequest.of("Say exactly: OK"),
                "OK\n",
                List.of("claude", "-p", "Say exactly: OK"),
                new CommandResult(0, "OK\n", "", Duration.ofMillis(42), false)
        );

        Path saved = writer.write(transcript);

        assertEquals("20260711T210000.123Z-550e8400-claude-cli.md", saved.getFileName().toString());
        String markdown = Files.readString(saved);
        assertTrue(markdown.contains("# AI Run"));
        assertTrue(markdown.contains("Run ID: 550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(markdown.contains("Backend: Claude CLI"));
        assertTrue(markdown.contains("Exit code: 0"));
        assertTrue(markdown.contains("Timed out: false"));
        assertTrue(markdown.contains("## Prompt"));
        assertTrue(markdown.contains("Say exactly: OK"));
        assertTrue(markdown.contains("## Response"));
        assertTrue(markdown.contains("OK"));
        assertTrue(markdown.contains("## Standard Error"));
    }

    @Test
    void usesFenceLongerThanContentBacktickRuns() {
        Transcript transcript = Transcript.success(
                "550e8400-e29b-41d4-a716-446655440000",
                new BackendId("Claude CLI"),
                Instant.parse("2026-07-11T21:00:00.123Z"),
                Duration.ofMillis(42),
                AiRequest.of("prompt with ``` fence"),
                "response with ```` longer fence",
                List.of("claude", "-p", "prompt with ``` fence"),
                new CommandResult(0, "response with ```` longer fence", "", Duration.ofMillis(42), false)
        );

        String markdown = TranscriptRenderer.render(transcript);

        assertTrue(markdown.contains("`````\nresponse with ```` longer fence\n`````"));
    }
}
