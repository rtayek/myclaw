package myclaw.application;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import myclaw.execution.CommandRequest;
import myclaw.execution.CommandResult;
import myclaw.execution.CommandRunner;
import myclaw.session.SessionEventStore;
import myclaw.transcript.Transcript;
import myclaw.transcript.TranscriptWriter;

public final class PromptService {
    private final Map<String, AiBackend> backends;
    private final Map<String, String> backendLabels;
    private final TranscriptWriter transcriptWriter;
    private final SessionEventStore sessionStore;
    private final Clock clock;

    public PromptService(Map<String, AiBackend> backends, TranscriptWriter transcriptWriter, Clock clock) {
        this(backends, backendIdsAsLabels(backends), transcriptWriter, null, clock);
    }

    public PromptService(
            Map<String, AiBackend> backends,
            Map<String, String> backendLabels,
            TranscriptWriter transcriptWriter,
            Clock clock
    ) {
        this(backends, backendLabels, transcriptWriter, null, clock);
    }

    public PromptService(
            Map<String, AiBackend> backends,
            Map<String, String> backendLabels,
            TranscriptWriter transcriptWriter,
            SessionEventStore sessionStore,
            Clock clock
    ) {
        this.backends = Map.copyOf(Objects.requireNonNull(backends, "backends"));
        this.backendLabels = Map.copyOf(Objects.requireNonNull(backendLabels, "backendLabels"));
        if (!this.backendLabels.keySet().containsAll(this.backends.keySet())) {
            throw new IllegalArgumentException("backendLabels must include every backend id");
        }
        this.transcriptWriter = Objects.requireNonNull(transcriptWriter, "transcriptWriter");
        this.sessionStore = sessionStore;
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

    public List<String> listSessions(String backendName) {
        if (!hasBackend(backendName)) {
            throw new IllegalArgumentException("Unknown backend: " + backendName);
        }

        List<String> sessions = new ArrayList<>();

        if (sessionStore != null) {
            try {
                sessionStore.listSessions().stream()
                        .map(summary -> summary.sessionId().value())
                        .forEach(sessions::add);
            } catch (RuntimeException ignored) {
                // Store unavailable, proceed to CLI discovery
            }
        }

        String binaryName = switch (backendName.toLowerCase()) {
            case "claude" -> "claude";
            case "codex" -> "codex";
            case "agy" -> "agy";
            default -> null;
        };

        if (binaryName != null) {
            try {
                CommandResult result = new CommandRunner().run(
                        new CommandRequest(List.of(binaryName, "--list-sessions"), "", Duration.ofSeconds(10))
                );
                if (result.exitCode() == 0 && !result.standardOutput().isBlank()) {
                    for (String line : result.standardOutput().split("\\r?\\n")) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !sessions.contains(trimmed)) {
                            sessions.add(trimmed);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return List.copyOf(sessions);
    }

    public PromptResult submit(String backendName, String prompt) {
        return submit(backendName, prompt, PromptProfile.GENERAL, null);
    }

    public PromptResult submit(String backendName, String prompt, PromptProfile profile) {
        return submit(backendName, prompt, profile, null);
    }

    public PromptResult submit(String backendName, String prompt, String sessionId) {
        return submit(backendName, prompt, PromptProfile.GENERAL, sessionId);
    }

    public PromptResult submit(String backendName, String prompt, PromptProfile profile, String sessionId) {
        AiBackend backend = backends.get(backendName);
        if (backend == null) {
            throw new IllegalArgumentException("Unknown backend: " + backendName);
        }

        AiRequest request = (sessionId != null && !sessionId.isBlank())
                ? AiRequest.withSession(prompt, sessionId, profile)
                : AiRequest.withProfile(prompt, profile);
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
