package myclaw.transcript;

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

public final class TranscriptWriter {
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX").withZone(ZoneOffset.UTC);

    private final Path runsDirectory;
    private final Clock clock;

    public TranscriptWriter(Path runsDirectory) {
        this(runsDirectory, Clock.systemUTC());
    }

    public TranscriptWriter(Path runsDirectory, Clock clock) {
        this.runsDirectory = Objects.requireNonNull(runsDirectory, "runsDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Path write(Transcript transcript) {
        Objects.requireNonNull(transcript, "transcript");
        Path path = runsDirectory.resolve(FILE_TIMESTAMP.format(clock.instant())
                + "-" + transcript.runId().substring(0, 8)
                + "-" + slugify(transcript.backendId().value()) + ".md");
        try {
            Files.createDirectories(runsDirectory);
            Files.writeString(path, TranscriptRenderer.render(transcript), StandardCharsets.UTF_8);
            return path;
        } catch (IOException exception) {
            throw new TranscriptWriteException("Could not write transcript " + path, exception);
        }
    }

    public static String newRunId() {
        return UUID.randomUUID().toString();
    }

    private static String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
