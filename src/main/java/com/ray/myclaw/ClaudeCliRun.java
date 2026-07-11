package com.ray.myclaw;

import java.util.List;
import java.util.Objects;

record ClaudeCliRun(
        AiResponse response,
        CommandResult commandResult,
        List<String> command
) {
    ClaudeCliRun {
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(commandResult, "commandResult");
        command = List.copyOf(Objects.requireNonNull(command, "command"));
    }
}
