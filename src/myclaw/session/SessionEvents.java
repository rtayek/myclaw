package myclaw.session;

import java.nio.file.Path;
import java.time.Instant;

import com.google.gson.JsonObject;

public final class SessionEvents {
    private SessionEvents() {
    }

    public static SessionEvent created(SessionId sessionId, long sequenceNumber, Instant occurredAt, String title) {
        JsonObject payload = new JsonObject();
        payload.addProperty("title", title);
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.SESSION_CREATED, payload);
    }

    public static SessionEvent userMessageAdded(SessionId sessionId, long sequenceNumber, Instant occurredAt, String text) {
        JsonObject payload = new JsonObject();
        payload.addProperty("text", text);
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.USER_MESSAGE_ADDED, payload);
    }

    public static SessionEvent backendSelected(SessionId sessionId, long sequenceNumber, Instant occurredAt, String backendName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("backendName", backendName);
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.BACKEND_SELECTED, payload);
    }

    public static SessionEvent backendRequestStarted(
            SessionId sessionId,
            long sequenceNumber,
            Instant occurredAt,
            String backendName,
            String prompt,
            String profileName
    ) {
        JsonObject payload = new JsonObject();
        payload.addProperty("backendName", backendName);
        payload.addProperty("prompt", prompt);
        payload.addProperty("profile", profileName);
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.BACKEND_REQUEST_STARTED, payload);
    }

    public static SessionEvent assistantMessageAdded(
            SessionId sessionId,
            long sequenceNumber,
            Instant occurredAt,
            String backendLabel,
            String text,
            Path transcriptPath
    ) {
        JsonObject payload = new JsonObject();
        payload.addProperty("backendLabel", backendLabel);
        payload.addProperty("text", text);
        payload.addProperty("transcriptPath", transcriptPath.toString());
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.ASSISTANT_MESSAGE_ADDED, payload);
    }

    public static SessionEvent backendRequestFailed(
            SessionId sessionId,
            long sequenceNumber,
            Instant occurredAt,
            String backendName,
            RuntimeException exception
    ) {
        JsonObject payload = new JsonObject();
        payload.addProperty("backendName", backendName);
        payload.addProperty("exceptionType", exception.getClass().getName());
        payload.addProperty("message", exception.getMessage() == null ? "" : exception.getMessage());
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.BACKEND_REQUEST_FAILED, payload);
    }

    public static SessionEvent statusChanged(
            SessionId sessionId,
            long sequenceNumber,
            Instant occurredAt,
            SessionStatus previousStatus,
            SessionStatus status
    ) {
        JsonObject payload = new JsonObject();
        payload.addProperty("previousStatus", previousStatus.name());
        payload.addProperty("status", status.name());
        return new SessionEvent(sessionId, sequenceNumber, occurredAt, SessionEventType.SESSION_STATUS_CHANGED, payload);
    }
}
