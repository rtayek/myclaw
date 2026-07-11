package com.ray.myclaw;

import java.util.Objects;
import java.util.Optional;

record AiRequest(String prompt, Optional<String> systemPrompt) {
    AiRequest {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(systemPrompt, "systemPrompt");
    }

    static AiRequest of(String prompt) {
        return new AiRequest(prompt, Optional.empty());
    }

    static AiRequest withSystemPrompt(String prompt, String systemPrompt) {
        return new AiRequest(prompt, Optional.of(systemPrompt));
    }
}
