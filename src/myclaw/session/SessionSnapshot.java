package myclaw.session;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SessionSnapshot(
        SessionId sessionId,
        String title,
        SessionStatus status,
        Optional<String> selectedBackend,
        List<SessionMessage> messages,
        List<SessionFailure> failures,
        List<SessionEvent> events
) {
    public SessionSnapshot {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(selectedBackend, "selectedBackend");
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
        events = List.copyOf(Objects.requireNonNull(events, "events"));
    }
}
