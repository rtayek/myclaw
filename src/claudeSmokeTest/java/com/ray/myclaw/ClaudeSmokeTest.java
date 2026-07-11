package com.ray.myclaw;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClaudeSmokeTest {
    @Test
    void claudePrintModeReturnsOk() {
        ClaudeCliBackend backend = new ClaudeCliBackend(new CommandRunner(), Duration.ofSeconds(30));

        AiResponse response = backend.ask(AiRequest.of("Say exactly: OK"));

        assertEquals("OK", response.text().trim());
    }
}
