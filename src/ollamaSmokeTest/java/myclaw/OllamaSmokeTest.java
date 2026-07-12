package myclaw;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class OllamaSmokeTest {
    @Test
    void ollamaGlmReturnsExpectedText() {
        OllamaCliBackend backend = new OllamaCliBackend(new CommandRunner(), Duration.ofMinutes(2), "glm4:9b");

        AiResponse response = backend.ask(AiRequest.of("Say exactly: GLM_SMOKE_OK"));

        assertTrue(response.text().contains("GLM_SMOKE_OK"));
    }
}
