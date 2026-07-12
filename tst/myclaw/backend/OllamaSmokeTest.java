package myclaw.backend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import myclaw.execution.CommandRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class OllamaSmokeTest {
    @Tag("ollamaSmoke")
    @Test
    void ollamaGlmReturnsExpectedText() {
        OllamaCliBackend backend = new OllamaCliBackend(new CommandRunner(), Duration.ofMinutes(2), "glm4:9b");

        AiResponse response = backend.ask(AiRequest.of("Say exactly: GLM_SMOKE_OK"));

        assertTrue(response.text().contains("GLM_SMOKE_OK"));
    }
}
