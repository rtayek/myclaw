package myclaw.session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SqliteSessionEventStore implements SessionEventStore {
    private static final Gson GSON = new Gson();

    private final Connection connection;

    public SqliteSessionEventStore(Path databasePath) {
        Objects.requireNonNull(databasePath, "databasePath");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            connection.setAutoCommit(true);
            initializeSchema();
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not open session store " + databasePath, exception);
        }
    }

    @Override
    public SessionId createSession(String title, Instant createdAt) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(createdAt, "createdAt");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        SessionId sessionId = SessionId.create();
        SessionEvent created = SessionEvents.created(sessionId, 1, createdAt, title);
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement insertSession = connection.prepareStatement("""
                    INSERT INTO session(session_id, title, created_at)
                    VALUES (?, ?, ?)
                    """);
                 PreparedStatement insertEvent = connection.prepareStatement("""
                    INSERT INTO session_event(session_id, sequence_number, event_type, occurred_at, payload_json)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                insertSession.setString(1, sessionId.value());
                insertSession.setString(2, title);
                insertSession.setString(3, createdAt.toString());
                insertSession.executeUpdate();
                bindEvent(insertEvent, created);
                insertEvent.executeUpdate();
                connection.commit();
                return sessionId;
            } catch (SQLException exception) {
                rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not create session", exception);
        }
    }

    @Override
    public void append(SessionEvent event) {
        Objects.requireNonNull(event, "event");
        try {
            connection.setAutoCommit(false);
            try {
                if (!sessionExists(event.sessionId())) {
                    throw new SessionStoreException("Unknown session: " + event.sessionId());
                }
                long expectedSequence = lastSequence(event.sessionId()) + 1L;
                if (event.sequenceNumber() != expectedSequence) {
                    throw new SessionStoreException("Expected sequence " + expectedSequence + " but got " + event.sequenceNumber());
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO session_event(session_id, sequence_number, event_type, occurred_at, payload_json)
                        VALUES (?, ?, ?, ?, ?)
                        """)) {
                    bindEvent(statement, event);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not append session event", exception);
        }
    }

    @Override
    public List<SessionEvent> load(SessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        if (!sessionExists(sessionId)) {
            throw new SessionStoreException("Unknown session: " + sessionId);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sequence_number, event_type, occurred_at, payload_json
                FROM session_event
                WHERE session_id = ?
                ORDER BY sequence_number
                """)) {
            statement.setString(1, sessionId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SessionEvent> events = new ArrayList<>();
                while (resultSet.next()) {
                    events.add(new SessionEvent(
                            sessionId,
                            resultSet.getLong("sequence_number"),
                            Instant.parse(resultSet.getString("occurred_at")),
                            SessionEventType.valueOf(resultSet.getString("event_type")),
                            JsonParser.parseString(resultSet.getString("payload_json")).getAsJsonObject()));
                }
                return List.copyOf(events);
            }
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not load session " + sessionId, exception);
        }
    }

    @Override
    public List<SessionSummary> listSessions() {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT s.session_id, s.title, s.created_at, MAX(e.occurred_at) AS last_event_at
                FROM session s
                JOIN session_event e ON e.session_id = s.session_id
                GROUP BY s.session_id, s.title, s.created_at
                ORDER BY s.created_at
                """);
             ResultSet resultSet = statement.executeQuery()) {
            List<SessionSummary> summaries = new ArrayList<>();
            while (resultSet.next()) {
                SessionId sessionId = new SessionId(resultSet.getString("session_id"));
                SessionSnapshot snapshot = SessionProjection.snapshot(load(sessionId));
                summaries.add(new SessionSummary(
                        sessionId,
                        resultSet.getString("title"),
                        Instant.parse(resultSet.getString("created_at")),
                        snapshot.status(),
                        Instant.parse(resultSet.getString("last_event_at"))));
            }
            return List.copyOf(summaries);
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not list sessions", exception);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not close session store", exception);
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS session (
                        session_id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS session_event (
                        session_id TEXT NOT NULL,
                        sequence_number INTEGER NOT NULL,
                        event_type TEXT NOT NULL,
                        occurred_at TEXT NOT NULL,
                        payload_json TEXT NOT NULL,
                        PRIMARY KEY (session_id, sequence_number),
                        FOREIGN KEY (session_id) REFERENCES session(session_id)
                    )
                    """);
        }
    }

    private boolean sessionExists(SessionId sessionId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM session WHERE session_id = ?")) {
            statement.setString(1, sessionId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not check session " + sessionId, exception);
        }
    }

    private long lastSequence(SessionId sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(MAX(sequence_number), 0) AS last_sequence
                FROM session_event
                WHERE session_id = ?
                """)) {
            statement.setString(1, sessionId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("last_sequence");
                }
                return 0;
            }
        }
    }

    private static void bindEvent(PreparedStatement statement, SessionEvent event) throws SQLException {
        statement.setString(1, event.sessionId().value());
        statement.setLong(2, event.sequenceNumber());
        statement.setString(3, event.eventType().name());
        statement.setString(4, event.occurredAt().toString());
        statement.setString(5, GSON.toJson(event.payload()));
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not roll back session transaction", exception);
        }
    }
}
