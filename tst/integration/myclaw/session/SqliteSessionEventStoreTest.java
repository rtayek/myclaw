package myclaw.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import myclaw.application.PromptService;
import myclaw.backend.AiBackend;
import myclaw.backend.AiBackendStartupException;
import myclaw.backend.AiRequest;
import myclaw.backend.AiResponse;
import myclaw.backend.BackendId;
import myclaw.transcript.TranscriptWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqliteSessionEventStoreTest {
    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T13:00:00Z"), ZoneOffset.UTC);

    @Test
    void durableStoreReloadsSameOrderedEventsAfterRestart() throws Exception {
        Path database = tempDir.resolve("sessions.db");
        SessionId sessionId;
        List<SessionEvent> before;

        try (SqliteSessionEventStore store = new SqliteSessionEventStore(database)) {
            SessionRuntime runtime = runtimeWith(store, request -> new AiResponse("persisted", new BackendId("Fake"), Duration.ofMillis(9)));
            sessionId = runtime.createSession("Durable");
            runtime.submit(sessionId, "fake", "remember this");
            before = store.load(sessionId);
        }

        try (SqliteSessionEventStore reopened = new SqliteSessionEventStore(database)) {
            List<SessionEvent> after = reopened.load(sessionId);
            SessionSnapshot snapshot = SessionProjection.snapshot(after);

            assertEquals(before, after);
            assertEquals("Durable", snapshot.title());
            assertEquals(SessionStatus.ACTIVE, snapshot.status());
            assertEquals("remember this", snapshot.messages().get(0).text());
            assertEquals("persisted", snapshot.messages().get(1).text());
            assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L), after.stream().map(SessionEvent::sequenceNumber).toList());
            assertTrue(Files.exists(database));
            assertEquals(1, reopened.listSessions().size());
        }
    }

    @Test
    void durableStoreKeepsFailureAndDoesNotExposeCauseTextThroughFailureProjection() {
        Path database = tempDir.resolve("sessions.db");
        SessionId sessionId;
        AiBackend backend = request -> {
            throw new AiBackendStartupException(
                    "Fake backend unavailable",
                    new BackendId("Fake"),
                    new IllegalStateException("SECRET=value"));
        };

        try (SqliteSessionEventStore store = new SqliteSessionEventStore(database)) {
            SessionRuntime runtime = runtimeWith(store, backend);
            sessionId = runtime.createSession("Failure");
            assertThrows(AiBackendStartupException.class, () -> runtime.submit(sessionId, "fake", "fail"));
        }

        try (SqliteSessionEventStore reopened = new SqliteSessionEventStore(database)) {
            SessionSnapshot snapshot = SessionProjection.snapshot(reopened.load(sessionId));

            assertEquals(SessionStatus.FAILED, snapshot.status());
            assertEquals(1, snapshot.failures().size());
            assertEquals("Fake backend unavailable", snapshot.failures().getFirst().message());
            assertTrue(snapshot.failures().stream().noneMatch(failure -> failure.message().contains("SECRET")));
        }
    }

    @Test
    void durableStoreRejectsOutOfOrderSequenceWithoutPartialAppend() {
        Path database = tempDir.resolve("sessions.db");

        try (SqliteSessionEventStore store = new SqliteSessionEventStore(database)) {
            SessionId sessionId = store.createSession("Sequence", clock.instant());

            assertThrows(SessionStoreException.class,
                    () -> store.append(SessionEvents.userMessageAdded(sessionId, 3, clock.instant(), "skip")));
            assertEquals(1, store.load(sessionId).size());
        }
    }

    private SessionRuntime runtimeWith(SessionEventStore store, AiBackend backend) {
        PromptService promptService = new PromptService(Map.of("fake", new CapturingBackend(backend)),
                new TranscriptWriter(tempDir.resolve("runs"), clock), clock);
        return new SessionRuntime(store, promptService, clock);
    }

    private static final class CapturingBackend implements AiBackend {
        private final AiBackend backend;
        private AiRequest request;

        private CapturingBackend(AiBackend backend) {
            this.backend = backend;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            this.request = request;
            return backend.ask(request);
        }
    }
}
