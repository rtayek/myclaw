package com.ray.myclaw;

import java.util.Optional;

final class AiBackendException extends RuntimeException {
    private final String backendName;
    private final CommandResult commandResult;

    AiBackendException(String message, String backendName, CommandResult commandResult) {
        super(message);
        this.backendName = backendName;
        this.commandResult = commandResult;
    }

    AiBackendException(String message, String backendName, Throwable cause) {
        super(message, cause);
        this.backendName = backendName;
        this.commandResult = null;
    }

    String backendName() {
        return backendName;
    }

    Optional<CommandResult> commandResult() {
        return Optional.ofNullable(commandResult);
    }
}
