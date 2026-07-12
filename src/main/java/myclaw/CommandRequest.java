package myclaw;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

record CommandRequest(
        List<String> command,
        String standardInput,
        Duration timeout
) {
    CommandRequest {
        Objects.requireNonNull(command, "command");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        command = List.copyOf(command);
        standardInput = standardInput == null ? "" : standardInput;
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
