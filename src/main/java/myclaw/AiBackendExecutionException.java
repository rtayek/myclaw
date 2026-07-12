package myclaw;

import java.util.Objects;
import java.util.Optional;

final class AiBackendExecutionException extends AiBackendException {
    private final CommandResult commandResult;

    AiBackendExecutionException(String message, BackendId backendId, CommandResult commandResult) {
        super(message, backendId);
        this.commandResult = Objects.requireNonNull(commandResult, "commandResult");
    }

    @Override
    Optional<CommandResult> commandResult() {
        return Optional.of(commandResult);
    }
}
