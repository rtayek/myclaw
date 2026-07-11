package com.ray.myclaw;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class HarnessMain {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private HarnessMain() {
    }

    public static void main(String[] args) {
        ClaudeCliBackend claudeBackend = new ClaudeCliBackend(new CommandRunner(), DEFAULT_TIMEOUT);
        int exitCode = new HarnessMainApplication(
                claudeBackend,
                new TranscriptWriter(Path.of("runs")),
                Clock.systemUTC()
        ).run(args);
        System.exit(exitCode);
    }
}

final class HarnessMainApplication {
    private final ClaudeCliBackend claudeBackend;
    private final TranscriptWriter transcriptWriter;
    private final Clock clock;

    HarnessMainApplication(ClaudeCliBackend claudeBackend, TranscriptWriter transcriptWriter, Clock clock) {
        this.claudeBackend = claudeBackend;
        this.transcriptWriter = transcriptWriter;
        this.clock = clock;
    }

    int run(String[] args) {
        if (args.length != 2 || !"claude".equals(args[0])) {
            System.err.println("Usage: java -jar ai-harness.jar claude \"prompt\"");
            return 2;
        }

        AiRequest request = AiRequest.of(args[1]);
        Instant started = clock.instant();
        String runId = TranscriptWriter.newRunId();
        try {
            ClaudeCliRun run = claudeBackend.askWithResult(request);
            System.out.print(run.response().text());
            writeSuccessfulTranscript(runId, started, request, run);
            return 0;
        } catch (AiBackendException exception) {
            writeFailedTranscript(runId, started, request, exception);
            System.err.println(exception.getMessage());
            exception.commandResult()
                    .map(CommandResult::standardError)
                    .filter(stderr -> !stderr.isBlank())
                    .ifPresent(System.err::print);
            return 1;
        } catch (TranscriptWriteException exception) {
            System.err.println(exception.getMessage());
            return 1;
        }
    }

    private void writeSuccessfulTranscript(String runId, Instant started, AiRequest request, ClaudeCliRun run) {
        Transcript transcript = new Transcript(
                runId,
                run.response().backendName(),
                started,
                run.response().duration(),
                request,
                run.response().text(),
                run.command(),
                run.commandResult(),
                null
        );
        transcriptWriter.write(transcript);
    }

    private void writeFailedTranscript(String runId, Instant started, AiRequest request, AiBackendException exception) {
        CommandResult commandResult = exception.commandResult().orElse(null);
        Duration duration = commandResult == null ? Duration.between(started, clock.instant()) : commandResult.duration();
        try {
            Transcript transcript = new Transcript(
                    runId,
                    exception.backendName(),
                    started,
                    duration,
                    request,
                    commandResult == null ? "" : commandResult.standardOutput(),
                    List.of("claude", "-p", request.prompt()),
                    commandResult,
                    exception.getMessage()
            );
            transcriptWriter.write(transcript);
        } catch (TranscriptWriteException transcriptException) {
            System.err.println(transcriptException.getMessage());
        }
    }
}
