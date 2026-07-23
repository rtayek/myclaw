package myclaw.session;

import java.time.Instant;
import java.util.Objects;

public record SessionSummary(
        SessionId sessionId,
        String title,
        Instant createdAt,
        SessionStatus status,
        Instant lastEventAt
) {
    public SessionSummary {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(lastEventAt, "lastEventAt");
    }
}
