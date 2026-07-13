package myclaw.backend;

import java.util.Objects;
import java.util.Optional;

import myclaw.execution.CommandResult;

public sealed abstract class AiBackendException extends RuntimeException
        permits AiBackendStartupException, AiBackendExecutionException, AiBackendUnsupportedRequestException {
    private static final long serialVersionUID = 1L;

    private final BackendId backendId;

    AiBackendException(String message, BackendId backendId) {
        super(message);
        this.backendId = Objects.requireNonNull(backendId, "backendId");
    }

    AiBackendException(String message, BackendId backendId, Throwable cause) {
        super(message, cause);
        this.backendId = Objects.requireNonNull(backendId, "backendId");
    }

    public BackendId backendId() {
        return backendId;
    }

    public Optional<CommandResult> commandResult() {
        return Optional.empty();
    }
}
