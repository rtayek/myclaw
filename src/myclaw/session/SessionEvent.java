package myclaw.session;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Objects;

public record SessionEvent(
        SessionId sessionId,
        long sequenceNumber,
        Instant occurredAt,
        SessionEventType eventType,
        JsonObject payload
) {
    public SessionEvent {
        Objects.requireNonNull(sessionId, "sessionId");
        if (sequenceNumber < 1) {
            throw new IllegalArgumentException("sequenceNumber must be positive");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(eventType, "eventType");
        payload = Objects.requireNonNull(payload, "payload").deepCopy();
    }

    @Override
    public JsonObject payload() {
        return payload.deepCopy();
    }
}
