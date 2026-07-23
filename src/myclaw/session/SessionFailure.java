package myclaw.session;

import java.time.Instant;
import java.util.Objects;

public record SessionFailure(
        String backendName,
        String exceptionType,
        String message,
        long sequenceNumber,
        Instant occurredAt
) {
    public SessionFailure {
        Objects.requireNonNull(backendName, "backendName");
        Objects.requireNonNull(exceptionType, "exceptionType");
        Objects.requireNonNull(message, "message");
        if (sequenceNumber < 1) {
            throw new IllegalArgumentException("sequenceNumber must be positive");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
