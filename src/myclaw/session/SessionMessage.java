package myclaw.session;

import java.time.Instant;
import java.util.Objects;

public record SessionMessage(
        SessionMessageRole role,
        String text,
        String backendLabel,
        long sequenceNumber,
        Instant occurredAt
) {
    public SessionMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(backendLabel, "backendLabel");
        if (sequenceNumber < 1) {
            throw new IllegalArgumentException("sequenceNumber must be positive");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
