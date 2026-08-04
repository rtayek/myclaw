package myclaw.backend;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import myclaw.execution.CommandExecutor;
import myclaw.execution.CommandRequest;
import myclaw.execution.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CodexCliBackendTest {
    private final CapturingExecutor executor = new CapturingExecutor();
    private final CodexCliBackend backend = new CodexCliBackend(executor, Duration.ofSeconds(5));

    @Test
    void constructsCodexPrintCommandWithPrompt() {
        executor.result = new CommandResult(0, "OK\n", "", Duration.ofMillis(5), false);

        AiResponse response = backend.ask(AiRequest.of("Hello Codex"));

        assertEquals("OK\n", response.text());
        assertEquals(List.of("codex", "-p", "Hello Codex"), executor.request.command());
    }

    @Test
    void constructsCodexResumeCommandWhenSessionIdIsPresent() {
        executor.result = new CommandResult(0, "Resumed", "", Duration.ofMillis(5), false);

        backend.ask(AiRequest.withSession("Continue task", "codex-sess-123"));

        assertEquals(List.of("codex", "--resume", "codex-sess-123", "-p", "Continue task"), executor.request.command());
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
