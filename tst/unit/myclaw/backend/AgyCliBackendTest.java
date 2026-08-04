package myclaw.backend;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import myclaw.execution.CommandExecutor;
import myclaw.execution.CommandRequest;
import myclaw.execution.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AgyCliBackendTest {
    private final CapturingExecutor executor = new CapturingExecutor();
    private final AgyCliBackend backend = new AgyCliBackend(executor, Duration.ofSeconds(5));

    @Test
    void constructsAgyPrintCommandWithPrompt() {
        executor.result = new CommandResult(0, "OK\n", "", Duration.ofMillis(5), false);

        AiResponse response = backend.ask(AiRequest.of("Hello Antigravity"));

        assertEquals("OK\n", response.text());
        assertEquals(List.of("agy", "-p", "Hello Antigravity"), executor.request.command());
    }

    @Test
    void constructsAgyResumeCommandWhenSessionIdIsPresent() {
        executor.result = new CommandResult(0, "Resumed", "", Duration.ofMillis(5), false);

        backend.ask(AiRequest.withSession("Continue agy task", "agy-sess-456"));

        assertEquals(List.of("agy", "--resume", "agy-sess-456", "-p", "Continue agy task"), executor.request.command());
    }

    private static final class CapturingExecutor implements CommandExecutor {
        CommandRequest request;
        CommandResult result;

        @Override
        public CommandResult run(CommandRequest request) {
            this.request = request;
            return result;
        }
    }
}
