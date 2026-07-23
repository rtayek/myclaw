package myclaw.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import myclaw.application.PromptResult;
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

final class SessionRuntimeTest {
    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsSessionWithCreatedEvent() {
        InMemorySessionEventStore store = new InMemorySessionEventStore();
        SessionRuntime runtime = runtimeWith(store, request -> new AiResponse("unused", new BackendId("Fake"), Duration.ZERO));

        SessionId sessionId = runtime.createSession("First task");
        SessionSnapshot snapshot = runtime.load(sessionId);

        assertEquals("First task", snapshot.title());
        assertEquals(SessionStatus.CREATED, snapshot.status());
        assertEquals(1, snapshot.events().size());
        assertEquals(SessionEventType.SESSION_CREATED, snapshot.events().getFirst().eventType());
    }

    @Test
    void recordsUserMessageBackendRequestAndAssistantResponseInOrder() {
        InMemorySessionEventStore store = new InMemorySessionEventStore();
        CapturingBackend backend = new CapturingBackend(new AiResponse("answer", new BackendId("Fake"), Duration.ofMillis(4)));
        SessionRuntime runtime = runtimeWith(store, backend);
        SessionId sessionId = runtime.createSession("Question");

        PromptResult result = runtime.submit(sessionId, "fake", "hello");
        SessionSnapshot snapshot = runtime.load(sessionId);

        assertEquals("answer", result.response());
        assertEquals("hello", backend.request.prompt());
        assertEquals(SessionStatus.ACTIVE, snapshot.status());
        assertEquals("fake", snapshot.selectedBackend().orElseThrow());
        assertEquals(2, snapshot.messages().size());
        assertEquals(SessionMessageRole.USER, snapshot.messages().get(0).role());
        assertEquals(SessionMessageRole.ASSISTANT, snapshot.messages().get(1).role());
        assertEquals(
                java.util.List.of(
                        SessionEventType.SESSION_CREATED,
                        SessionEventType.USER_MESSAGE_ADDED,
                        SessionEventType.BACKEND_SELECTED,
                        SessionEventType.SESSION_STATUS_CHANGED,
                        SessionEventType.BACKEND_REQUEST_STARTED,
                        SessionEventType.ASSISTANT_MESSAGE_ADDED),
                snapshot.events().stream().map(SessionEvent::eventType).toList());
        assertEquals(java.util.List.of(1L, 2L, 3L, 4L, 5L, 6L),
                snapshot.events().stream().map(SessionEvent::sequenceNumber).toList());
    }

    @Test
    void recordsFailureEventAndFailedStatusBeforeRethrowing() {
        InMemorySessionEventStore store = new InMemorySessionEventStore();
        AiBackendStartupException failure = new AiBackendStartupException(
                "Fake backend unavailable",
                new BackendId("Fake"),
                new IllegalStateException("internal token SECRET"));
        SessionRuntime runtime = runtimeWith(store, request -> {
            throw failure;
        });
        SessionId sessionId = runtime.createSession("Failure");

        assertThrows(AiBackendStartupException.class, () -> runtime.submit(sessionId, "fake", "fail"));
        SessionSnapshot snapshot = runtime.load(sessionId);

        assertEquals(SessionStatus.FAILED, snapshot.status());
        assertEquals(1, snapshot.failures().size());
        assertEquals("Fake backend unavailable", snapshot.failures().getFirst().message());
        assertTrue(snapshot.failures().getFirst().exceptionType().endsWith("AiBackendStartupException"));
        assertTrue(snapshot.failures().stream().noneMatch(item -> item.message().contains("SECRET")));
    }

    @Test
    void missingSessionThrows() {
        SessionRuntime runtime = runtimeWith(new InMemorySessionEventStore(),
                request -> new AiResponse("unused", new BackendId("Fake"), Duration.ZERO));

        assertThrows(SessionStoreException.class, () -> runtime.load(new SessionId("missing")));
    }

    @Test
    void storeRejectsDuplicateOrInvalidSequence() {
        InMemorySessionEventStore store = new InMemorySessionEventStore();
        SessionId sessionId = store.createSession("Sequence", clock.instant());

        SessionStoreException duplicate = assertThrows(SessionStoreException.class,
                () -> store.append(SessionEvents.userMessageAdded(sessionId, 1, clock.instant(), "duplicate")));
        assertTrue(duplicate.getMessage().contains("Expected sequence 2"));
    }

    private SessionRuntime runtimeWith(SessionEventStore store, AiBackend backend) {
        PromptService promptService = new PromptService(Map.of("fake", backend), new TranscriptWriter(tempDir, clock), clock);
        return new SessionRuntime(store, promptService, clock);
    }

    private static final class CapturingBackend implements AiBackend {
        private final AiResponse response;
        private AiRequest request;

        private CapturingBackend(AiResponse response) {
            this.response = response;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            this.request = request;
            return response;
        }
    }
}
