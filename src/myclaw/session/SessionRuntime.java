package myclaw.session;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import myclaw.application.PromptResult;
import myclaw.application.PromptService;
import myclaw.backend.PromptProfile;

public final class SessionRuntime implements AutoCloseable {
    private final SessionEventStore store;
    private final PromptService promptService;
    private final Clock clock;

    public SessionRuntime(SessionEventStore store, PromptService promptService, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.promptService = Objects.requireNonNull(promptService, "promptService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SessionId createSession(String title) {
        return store.createSession(title, clock.instant());
    }

    public SessionSnapshot load(SessionId sessionId) {
        return SessionProjection.snapshot(store.load(sessionId));
    }

    public PromptResult submit(SessionId sessionId, String backendName, String prompt) {
        return submit(sessionId, backendName, prompt, PromptProfile.GENERAL);
    }

    public PromptResult submit(SessionId sessionId, String backendName, String prompt, PromptProfile profile) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(backendName, "backendName");
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(profile, "profile");

        SessionSnapshot before = load(sessionId);
        long sequence = nextSequence(before.events());
        Instant started = clock.instant();
        store.append(SessionEvents.userMessageAdded(sessionId, sequence++, started, prompt));
        if (before.selectedBackend().isEmpty() || !before.selectedBackend().orElseThrow().equals(backendName)) {
            store.append(SessionEvents.backendSelected(sessionId, sequence++, started, backendName));
        }
        if (before.status() == SessionStatus.CREATED) {
            store.append(SessionEvents.statusChanged(sessionId, sequence++, started, SessionStatus.CREATED, SessionStatus.ACTIVE));
        }
        store.append(SessionEvents.backendRequestStarted(sessionId, sequence++, started, backendName, prompt, profile.name()));

        try {
            PromptResult result = promptService.submit(backendName, prompt, profile);
            store.append(SessionEvents.assistantMessageAdded(
                    sessionId,
                    sequence,
                    clock.instant(),
                    result.backendLabel(),
                    result.response(),
                    result.transcriptPath()));
            return result;
        } catch (RuntimeException exception) {
            store.append(SessionEvents.backendRequestFailed(sessionId, sequence++, clock.instant(), backendName, exception));
            store.append(SessionEvents.statusChanged(sessionId, sequence, clock.instant(), SessionStatus.ACTIVE, SessionStatus.FAILED));
            throw exception;
        }
    }

    @Override
    public void close() {
        store.close();
    }

    private static long nextSequence(List<SessionEvent> events) {
        return events.getLast().sequenceNumber() + 1L;
    }
}
