package myclaw.session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;

public final class SessionProjection {
    private SessionProjection() {
    }

    public static SessionSnapshot snapshot(List<SessionEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }

        List<SessionEvent> ordered = events.stream()
                .sorted(Comparator.comparingLong(SessionEvent::sequenceNumber))
                .toList();
        SessionId sessionId = ordered.getFirst().sessionId();
        String title = "";
        SessionStatus status = SessionStatus.CREATED;
        Optional<String> selectedBackend = Optional.empty();
        List<SessionMessage> messages = new ArrayList<>();
        List<SessionFailure> failures = new ArrayList<>();

        for (SessionEvent event : ordered) {
            if (!event.sessionId().equals(sessionId)) {
                throw new IllegalArgumentException("events must belong to one session");
            }
            JsonObject payload = event.payload();
            switch (event.eventType()) {
                case SESSION_CREATED -> title = requiredString(payload, "title");
                case USER_MESSAGE_ADDED -> {
                    messages.add(new SessionMessage(
                            SessionMessageRole.USER,
                            requiredString(payload, "text"),
                            "",
                            event.sequenceNumber(),
                            event.occurredAt()));
                    if (status == SessionStatus.CREATED) {
                        status = SessionStatus.ACTIVE;
                    }
                }
                case BACKEND_SELECTED -> selectedBackend = Optional.of(requiredString(payload, "backendName"));
                case ASSISTANT_MESSAGE_ADDED -> messages.add(new SessionMessage(
                        SessionMessageRole.ASSISTANT,
                        requiredString(payload, "text"),
                        requiredString(payload, "backendLabel"),
                        event.sequenceNumber(),
                        event.occurredAt()));
                case BACKEND_REQUEST_FAILED -> failures.add(new SessionFailure(
                        requiredString(payload, "backendName"),
                        requiredString(payload, "exceptionType"),
                        requiredString(payload, "message"),
                        event.sequenceNumber(),
                        event.occurredAt()));
                case SESSION_STATUS_CHANGED -> status = SessionStatus.valueOf(requiredString(payload, "status"));
                case BACKEND_REQUEST_STARTED -> {
                }
            }
        }

        return new SessionSnapshot(sessionId, title, status, selectedBackend, messages, failures, ordered);
    }

    private static String requiredString(JsonObject payload, String name) {
        if (!payload.has(name) || payload.get(name).isJsonNull()) {
            throw new IllegalArgumentException("event payload is missing " + name);
        }
        return payload.get(name).getAsString();
    }
}
