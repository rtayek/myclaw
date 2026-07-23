package myclaw.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InMemorySessionEventStore implements SessionEventStore {
    private final Map<SessionId, StoredSession> sessions = new LinkedHashMap<>();

    @Override
    public synchronized SessionId createSession(String title, Instant createdAt) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        SessionId sessionId = SessionId.create();
        StoredSession session = new StoredSession(title, createdAt);
        session.events.add(SessionEvents.created(sessionId, 1, createdAt, title));
        sessions.put(sessionId, session);
        return sessionId;
    }

    @Override
    public synchronized void append(SessionEvent event) {
        StoredSession session = sessions.get(Objects.requireNonNull(event, "event").sessionId());
        if (session == null) {
            throw new SessionStoreException("Unknown session: " + event.sessionId());
        }
        long expectedSequence = session.events.size() + 1L;
        if (event.sequenceNumber() != expectedSequence) {
            throw new SessionStoreException("Expected sequence " + expectedSequence + " but got " + event.sequenceNumber());
        }
        session.events.add(event);
    }

    @Override
    public synchronized List<SessionEvent> load(SessionId sessionId) {
        StoredSession session = sessions.get(Objects.requireNonNull(sessionId, "sessionId"));
        if (session == null) {
            throw new SessionStoreException("Unknown session: " + sessionId);
        }
        return List.copyOf(session.events);
    }

    @Override
    public synchronized List<SessionSummary> listSessions() {
        return sessions.entrySet().stream()
                .map(entry -> {
                    SessionSnapshot snapshot = SessionProjection.snapshot(entry.getValue().events);
                    return new SessionSummary(
                            entry.getKey(),
                            entry.getValue().title,
                            entry.getValue().createdAt,
                            snapshot.status(),
                            entry.getValue().events.getLast().occurredAt());
                })
                .sorted(Comparator.comparing(SessionSummary::createdAt))
                .toList();
    }

    @Override
    public void close() {
    }

    private static final class StoredSession {
        private final String title;
        private final Instant createdAt;
        private final List<SessionEvent> events = new ArrayList<>();

        private StoredSession(String title, Instant createdAt) {
            this.title = title;
            this.createdAt = createdAt;
        }
    }
}
