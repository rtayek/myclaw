package myclaw.backend;

import java.util.Objects;
import java.util.Optional;

public record AiRequest(String prompt, Optional<String> systemPrompt) {
    public AiRequest {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(systemPrompt, "systemPrompt");
    }

    public static AiRequest of(String prompt) {
        return new AiRequest(prompt, Optional.empty());
    }

    public static AiRequest withSystemPrompt(String prompt, String systemPrompt) {
        return new AiRequest(prompt, Optional.of(systemPrompt));
    }
}
