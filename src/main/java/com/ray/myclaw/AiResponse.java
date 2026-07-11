package com.ray.myclaw;

import java.time.Duration;
import java.util.Objects;

record AiResponse(
        String text,
        BackendId backendId,
        Duration duration
) {
    AiResponse {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(backendId, "backendId");
        Objects.requireNonNull(duration, "duration");
    }
}
