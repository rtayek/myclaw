package myclaw;

import java.time.Duration;
import java.util.Objects;

record CommandResult(
        int exitCode,
        String standardOutput,
        String standardError,
        Duration duration,
        boolean timedOut
) {
    CommandResult {
        Objects.requireNonNull(standardOutput, "standardOutput");
        Objects.requireNonNull(standardError, "standardError");
        Objects.requireNonNull(duration, "duration");
    }
}
