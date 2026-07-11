package com.ray.myclaw;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

final class TranscriptWriter {
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX").withZone(ZoneOffset.UTC);

    private final Path runsDirectory;
    private final Clock clock;

    TranscriptWriter(Path runsDirectory) {
        this(runsDirectory, Clock.systemUTC());
    }

    TranscriptWriter(Path runsDirectory, Clock clock) {
        this.runsDirectory = Objects.requireNonNull(runsDirectory, "runsDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    Path write(Transcript transcript) {
        Objects.requireNonNull(transcript, "transcript");
        String backendSlug = transcript.backendName().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        Path path = runsDirectory.resolve(FILE_TIMESTAMP.format(clock.instant()) + "-" + transcript.runId().substring(0, 8) + "-" + backendSlug + ".md");
        try {
            Files.createDirectories(runsDirectory);
            Files.writeString(path, render(transcript), StandardCharsets.UTF_8);
            return path;
        } catch (IOException exception) {
            throw new TranscriptWriteException("Could not write transcript " + path, exception);
        }
    }

    static String newRunId() {
        return UUID.randomUUID().toString();
    }

    static String render(Transcript transcript) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# AI Run\n\n");
        markdown.append("Run ID: ").append(transcript.runId()).append("\n");
        markdown.append("Backend: ").append(transcript.backendName()).append("\n");
        markdown.append("Started: ").append(transcript.started()).append("\n");
        markdown.append("Duration: ").append(transcript.duration()).append("\n");
        if (transcript.commandResult() != null) {
            markdown.append("Exit code: ").append(transcript.commandResult().exitCode()).append("\n");
            markdown.append("Timed out: ").append(transcript.commandResult().timedOut()).append("\n");
        }
        if (transcript.errorMessage() != null) {
            markdown.append("Error: ").append(transcript.errorMessage()).append("\n");
        }

        appendSection(markdown, "Prompt", transcript.request().prompt());
        appendSection(markdown, "Response", transcript.responseText());
        appendSection(markdown, "Command", String.join("\n", transcript.command()));
        appendSection(markdown, "Standard Error", transcript.commandResult() == null ? "" : transcript.commandResult().standardError());
        return markdown.toString();
    }

    private static void appendSection(StringBuilder markdown, String heading, String content) {
        markdown.append("\n## ").append(heading).append("\n\n");
        String fence = fenceFor(content);
        markdown.append(fence).append("\n");
        markdown.append(content);
        if (!content.endsWith("\n")) {
            markdown.append("\n");
        }
        markdown.append(fence).append("\n");
    }

    private static String fenceFor(String content) {
        int longestRun = 0;
        int currentRun = 0;
        for (int index = 0; index < content.length(); index++) {
            if (content.charAt(index) == '`') {
                currentRun++;
                longestRun = Math.max(longestRun, currentRun);
            } else {
                currentRun = 0;
            }
        }
        return "`".repeat(Math.max(3, longestRun + 1));
    }
}
