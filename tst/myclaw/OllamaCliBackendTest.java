package myclaw;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OllamaCliBackendTest {
    private final CapturingExecutor executor = new CapturingExecutor();
    private final OllamaCliBackend backend = new OllamaCliBackend(executor, Duration.ofMinutes(2), "glm4:9b");

    @Test
    void constructsOllamaCommandWithoutShellOrPromptArgument() {
        executor.result = new CommandResult(0, "ok", "", Duration.ofMillis(1), false);

        backend.ask(AiRequest.of("Say exactly: OK"));

        assertEquals(List.of("ollama", "run", "glm4:9b"), executor.request.command());
        assertEquals(3, executor.request.command().size());
    }

    @Test
    void sendsExactPromptThroughStandardInput() {
        String prompt = """
                first line
                second line
                "quotes"; $HOME `command`
                caf\u00e9 \u03c0 \u65e5\u672c\u8a9e
                """;
        executor.result = new CommandResult(0, "ok", "", Duration.ofMillis(1), false);

        backend.ask(AiRequest.of(prompt));

        assertEquals(prompt, executor.request.standardInput());
    }

    @Test
    void successfulOutputBecomesAiResponseText() {
        executor.result = new CommandResult(0, "GLM_OK\n", "", Duration.ofMillis(12), false);

        AiResponse response = backend.ask(AiRequest.of("Say exactly: GLM_OK"));

        assertEquals("GLM_OK\n", response.text());
        assertEquals("Ollama glm4:9b", response.backendId().value());
        assertEquals(Duration.ofMillis(12), response.duration());
    }

    @Test
    void nonzeroCommandResultBecomesExecutionFailure() {
        executor.result = new CommandResult(3, "", "model failed", Duration.ofMillis(2), false);

        AiBackendException exception = assertThrows(AiBackendException.class, () -> backend.ask(AiRequest.of("hello")));

        assertInstanceOf(AiBackendExecutionException.class, exception);
        assertEquals("Ollama glm4:9b exited with status 3: model failed", exception.getMessage());
        assertEquals(3, exception.commandResult().orElseThrow().exitCode());
        assertEquals("model failed", exception.commandResult().orElseThrow().standardError());
    }

    @Test
    void timeoutBecomesExecutionFailure() {
        executor.result = new CommandResult(-1, "partial", "", Duration.ofMinutes(2), true);

        AiBackendException exception = assertThrows(AiBackendException.class, () -> backend.ask(AiRequest.of("hello")));

        assertInstanceOf(AiBackendExecutionException.class, exception);
        assertTrue(exception.getMessage().contains("timed out"));
        assertTrue(exception.commandResult().orElseThrow().timedOut());
    }

    @Test
    void startupFailurePreservesCause() {
        RuntimeException cause = new RuntimeException("missing executable");
        executor.failure = new CommandExecutionException("Could not start command ollama", cause);

        AiBackendException exception = assertThrows(AiBackendException.class, () -> backend.ask(AiRequest.of("hello")));

        assertInstanceOf(AiBackendStartupException.class, exception);
        assertTrue(exception.getMessage().contains("Could not start Ollama glm4:9b"));
        assertSame(executor.failure, exception.getCause());
    }

    @Test
    void systemPromptIsRejectedWithoutRunningExecutor() {
        AiRequest request = AiRequest.withSystemPrompt("hello", "system");

        AiBackendException exception = assertThrows(AiBackendException.class, () -> backend.ask(request));

        assertInstanceOf(AiBackendUnsupportedRequestException.class, exception);
        assertNull(executor.request);
    }

    private static final class CapturingExecutor implements CommandExecutor {
        CommandRequest request;
        CommandResult result;
        CommandExecutionException failure;

        @Override
        public CommandResult run(CommandRequest request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}
