package com.ray.myclaw;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

record Transcript(
        String runId,
        String backendName,
        Instant started,
        Duration duration,
        AiRequest request,
        String responseText,
        List<String> command,
        CommandResult commandResult,
        String errorMessage
) {
    Transcript {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(backendName, "backendName");
        Objects.requireNonNull(started, "started");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(request, "request");
        responseText = responseText == null ? "" : responseText;
        command = List.copyOf(Objects.requireNonNull(command, "command"));
    }
}
