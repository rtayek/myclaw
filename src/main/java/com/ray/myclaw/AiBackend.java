package com.ray.myclaw;

interface AiBackend {
    AiResponse ask(AiRequest request);

    default boolean supportsSystemPrompt() {
        return false;
    }
}
