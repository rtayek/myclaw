package com.ray.myclaw;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class HarnessMain {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private HarnessMain() {
    }

    public static void main(String[] args) {
        Map<String, AiBackend> backends = Map.of(
                "claude", new ClaudeCliBackend(new CommandRunner(), DEFAULT_TIMEOUT)
        );
        int exitCode = new HarnessMainApplication(
                backends,
                new TranscriptWriter(Path.of("runs")),
                Clock.systemUTC(),
                new ResultReporter(System.out, System.err)
        ).run(args);
        System.exit(exitCode);
    }
}

final class HarnessMainApplication {
    private static final String USAGE = "Usage: java -jar ai-harness.jar <backend> \"prompt\"";

    private final Map<String, AiBackend> backends;
    private final TranscriptWriter transcriptWriter;
    private final Clock clock;
    private final ResultReporter reporter;

    HarnessMainApplication(
            Map<String, AiBackend> backends,
            TranscriptWriter transcriptWriter,
            Clock clock,
            ResultReporter reporter
    ) {
        this.backends = backends;
        this.transcriptWriter = transcriptWriter;
        this.clock = clock;
        this.reporter = reporter;
    }

    int run(String[] args) {
        if (args.length != 2) {
            reporter.reportUsageError(USAGE);
            return 2;
        }

        AiBackend backend = backends.get(args[0]);
        if (backend == null) {
            reporter.reportUsageError(USAGE);
            return 2;
        }

        AiRequest request = AiRequest.of(args[1]);
        Instant started = clock.instant();
        String runId = TranscriptWriter.newRunId();
        try {
            if (backend instanceof ClaudeCliBackend cliBackend) {
                ClaudeCliRun run = cliBackend.askWithResult(request);
                reporter.reportSuccess(run.response());
                writeSuccessfulTranscript(runId, started, request, run.response(), run.command(), run.commandResult());
            } else {
                AiResponse response = backend.ask(request);
                reporter.reportSuccess(response);
                writeSuccessfulTranscript(runId, started, request, response, List.of(), null);
            }
            return 0;
        } catch (AiBackendException exception) {
            writeFailedTranscript(runId, started, request, failedCommandFor(backend, request), exception);
            reporter.reportFailure(exception);
            return 1;
        } catch (TranscriptWriteException exception) {
            reporter.reportTranscriptWriteFailure(exception);
            return 1;
        }
    }

    private void writeSuccessfulTranscript(
            String runId,
            Instant started,
            AiRequest request,
            AiResponse response,
            List<String> command,
            CommandResult commandResult
    ) {
        Transcript transcript = Transcript.success(
                runId,
                response.backendId(),
                started,
                response.duration(),
                request,
                response.text(),
                command,
                commandResult
        );
        transcriptWriter.write(transcript);
    }

    private void writeFailedTranscript(
            String runId,
            Instant started,
            AiRequest request,
            List<String> command,
            AiBackendException exception
    ) {
        CommandResult commandResult = exception.commandResult().orElse(null);
        Duration duration = commandResult == null ? Duration.between(started, clock.instant()) : commandResult.duration();
        try {
            Transcript transcript = Transcript.failure(
                    runId,
                    exception.backendId(),
                    started,
                    duration,
                    request,
                    commandResult == null ? "" : commandResult.standardOutput(),
                    command,
                    commandResult,
                    exception.getMessage()
            );
            transcriptWriter.write(transcript);
        } catch (TranscriptWriteException transcriptException) {
            reporter.reportTranscriptWriteFailure(transcriptException);
        }
    }

    private static List<String> failedCommandFor(AiBackend backend, AiRequest request) {
        if (backend instanceof ClaudeCliBackend) {
            return ClaudeCliBackend.commandFor(request);
        }
        return List.of();
    }
}
