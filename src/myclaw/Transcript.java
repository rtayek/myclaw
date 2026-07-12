package myclaw;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

record Transcript(
        String runId,
        BackendId backendId,
        Instant started,
        Duration duration,
        AiRequest request,
        String responseText,
        List<String> command,
        Optional<CommandResult> commandResult,
        Optional<String> errorMessage
) {
    Transcript {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(backendId, "backendId");
        Objects.requireNonNull(started, "started");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(request, "request");
        responseText = responseText == null ? "" : responseText;
        command = List.copyOf(Objects.requireNonNull(command, "command"));
        Objects.requireNonNull(commandResult, "commandResult");
        Objects.requireNonNull(errorMessage, "errorMessage");
    }

    static Transcript success(
            String runId,
            BackendId backendId,
            Instant started,
            Duration duration,
            AiRequest request,
            String responseText,
            List<String> command,
            CommandResult commandResult
    ) {
        return new Transcript(
                runId, backendId, started, duration, request, responseText, command,
                Optional.ofNullable(commandResult), Optional.empty());
    }

    static Transcript failure(
            String runId,
            BackendId backendId,
            Instant started,
            Duration duration,
            AiRequest request,
            String responseText,
            List<String> command,
            CommandResult commandResult,
            String errorMessage
    ) {
        return new Transcript(
                runId, backendId, started, duration, request, responseText, command,
                Optional.ofNullable(commandResult), Optional.of(errorMessage));
    }
}
