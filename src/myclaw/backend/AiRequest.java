package myclaw.backend;

import java.util.Objects;
import java.util.Optional;

public record AiRequest(String prompt, Optional<String> systemPrompt, PromptProfile profile) {
    public AiRequest(String prompt, Optional<String> systemPrompt) {
        this(prompt, systemPrompt, PromptProfile.GENERAL);
    }

    public AiRequest {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        Objects.requireNonNull(profile, "profile");
    }

    public static AiRequest of(String prompt) {
        return new AiRequest(prompt, Optional.empty(), PromptProfile.GENERAL);
    }

    public static AiRequest withProfile(String prompt, PromptProfile profile) {
        return new AiRequest(prompt, Optional.empty(), profile);
    }

    public static AiRequest withSystemPrompt(String prompt, String systemPrompt) {
        return new AiRequest(prompt, Optional.of(systemPrompt), PromptProfile.GENERAL);
    }

    String effectivePrompt() {
        return profile.applyTo(prompt);
    }
}
