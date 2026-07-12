package myclaw.application;

import java.time.Duration;
import java.util.Map;

import myclaw.backend.AiBackend;
import myclaw.backend.ClaudeCliBackend;
import myclaw.backend.OllamaCliBackend;
import myclaw.execution.CommandRunner;

public final class ApplicationBackends {
    private static final Duration CLAUDE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration OLLAMA_TIMEOUT = Duration.ofMinutes(2);

    private ApplicationBackends() {
    }

    public static Map<String, AiBackend> create() {
        return Map.of(
                "claude", new ClaudeCliBackend(new CommandRunner(), CLAUDE_TIMEOUT),
                "glm", new OllamaCliBackend(new CommandRunner(), OLLAMA_TIMEOUT, "glm4:9b")
        );
    }
}
