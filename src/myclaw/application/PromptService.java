package myclaw.application;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import myclaw.backend.AiBackend;
import myclaw.backend.AiBackendException;
import myclaw.backend.AiRequest;
import myclaw.backend.AiResponse;
import myclaw.backend.CommandBackedAiBackend;
import myclaw.backend.CommandBackedRun;
import myclaw.backend.PromptProfile;
import myclaw.execution.CommandResult;
import myclaw.transcript.Transcript;
import myclaw.transcript.TranscriptWriter;

public final class PromptService {
    private final Map<String, AiBackend> backends;
    private final Map<String, String> backendLabels;
    private final TranscriptWriter transcriptWriter;
    private final Clock clock;

    public PromptService(Map<String, AiBackend> backends, TranscriptWriter transcriptWriter, Clock clock) {
        this(backends, backendIdsAsLabels(backends), transcriptWriter, clock);
    }

    public PromptService(
            Map<String, AiBackend> backends,
            Map<String, String> backendLabels,
            TranscriptWriter transcriptWriter,
            Clock clock
    ) {
        this.backends = Map.copyOf(Objects.requireNonNull(backends, "backends"));
        this.backendLabels = Map.copyOf(Objects.requireNonNull(backendLabels, "backendLabels"));
        if (!this.backendLabels.keySet().containsAll(this.backends.keySet())) {
            throw new IllegalArgumentException("backendLabels must include every backend id");
        }
        this.transcriptWriter = Objects.requireNonNull(transcriptWriter, "transcriptWriter");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public boolean hasBackend(String backendName) {
        return backends.containsKey(backendName);
    }

    public List<BackendDescriptor> backends() {
        return new TreeMap<>(backendLabels).entrySet().stream()
                .filter(entry -> backends.containsKey(entry.getKey()))
                .map(entry -> new BackendDescriptor(entry.getKey(), entry.getValue()))
                .toList();
    }

    public PromptResult submit(String backendName, String prompt) {
        return submit(backendName, prompt, PromptProfile.GENERAL);
    }

    public PromptResult submit(String backendName, String prompt, PromptProfile profile) {
        AiBackend backend = backends.get(backendName);
        if (backend == null) {
            throw new IllegalArgumentException("Unknown backend: " + backendName);
        }

        AiRequest request = AiRequest.withProfile(prompt, profile);
        Instant started = clock.instant();
        String runId = TranscriptWriter.newRunId();
        try {
            if (backend instanceof CommandBackedAiBackend commandBackedBackend) {
                CommandBackedRun run = commandBackedBackend.askWithResult(request);
                Path transcriptPath = writeSuccessfulTranscript(runId, started, request, run.response(), run.command(), run.commandResult());
                return new PromptResult(run.response().backendId().value(), run.response().text(), transcriptPath);
            }

            AiResponse response = backend.ask(request);
            Path transcriptPath = writeSuccessfulTranscript(runId, started, request, response, List.of(), null);
            return new PromptResult(response.backendId().value(), response.text(), transcriptPath);
        } catch (AiBackendException exception) {
            writeFailedTranscript(runId, started, request, failedCommandFor(backend, request), exception);
            throw exception;
        }
    }

    private Path writeSuccessfulTranscript(
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
        return transcriptWriter.write(transcript);
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
    }

    private static List<String> failedCommandFor(AiBackend backend, AiRequest request) {
        if (backend instanceof CommandBackedAiBackend commandBackedBackend) {
            return commandBackedBackend.commandFor(request);
        }
        return List.of();
    }

    private static Map<String, String> backendIdsAsLabels(Map<String, AiBackend> backends) {
        Objects.requireNonNull(backends, "backends");
        return backends.keySet().stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> id));
    }
}
