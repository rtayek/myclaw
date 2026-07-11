package com.ray.myclaw;

import java.util.Objects;
import java.util.Optional;

sealed abstract class AiBackendException extends RuntimeException
        permits AiBackendStartupException, AiBackendExecutionException, AiBackendUnsupportedRequestException {
    private final BackendId backendId;

    AiBackendException(String message, BackendId backendId) {
        super(message);
        this.backendId = Objects.requireNonNull(backendId, "backendId");
    }

    AiBackendException(String message, BackendId backendId, Throwable cause) {
        super(message, cause);
        this.backendId = Objects.requireNonNull(backendId, "backendId");
    }

    BackendId backendId() {
        return backendId;
    }

    Optional<CommandResult> commandResult() {
        return Optional.empty();
    }
}
