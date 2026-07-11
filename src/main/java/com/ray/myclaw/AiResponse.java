package com.ray.myclaw;

import java.time.Duration;
import java.util.Objects;

record AiResponse(
        String text,
        String backendName,
        Duration duration
) {
    AiResponse {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(backendName, "backendName");
        Objects.requireNonNull(duration, "duration");
    }
}
