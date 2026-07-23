package myclaw.session;

import java.time.Instant;
import java.util.List;

public interface SessionEventStore extends AutoCloseable {
    SessionId createSession(String title, Instant createdAt);

    void append(SessionEvent event);

    List<SessionEvent> load(SessionId sessionId);

    List<SessionSummary> listSessions();

    @Override
    void close();
}
