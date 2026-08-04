package myclaw.application;

import java.time.Duration;
import java.util.Map;

import myclaw.backend.AgyCliBackend;
import myclaw.backend.AiBackend;
import myclaw.backend.ClaudeCliBackend;
import myclaw.backend.CodexCliBackend;
import myclaw.backend.OllamaCliBackend;
import myclaw.execution.CommandRunner;

public final class ApplicationBackends {
    private static final Duration CLAUDE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration CODEX_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration AGY_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration OLLAMA_TIMEOUT = Duration.ofMinutes(2);

    private ApplicationBackends() {
    }

    public static Map<String, AiBackend> create() {
        CommandRunner runner = new CommandRunner();
        return Map.of(
                "claude", new ClaudeCliBackend(runner, CLAUDE_TIMEOUT),
                "codex", new CodexCliBackend(runner, CODEX_TIMEOUT),
                "agy", new AgyCliBackend(runner, AGY_TIMEOUT),
                "glm", new OllamaCliBackend(runner, OLLAMA_TIMEOUT, "glm4:9b")
        );
    }
}
