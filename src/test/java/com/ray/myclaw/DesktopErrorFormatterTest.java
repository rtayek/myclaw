package com.ray.myclaw;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DesktopErrorFormatterTest {
    @Test
    void backendErrorIncludesStderrWhenMessageDoesNotAlreadyContainIt() {
        AiBackendException exception = new AiBackendExecutionException(
                "Ollama glm4:9b exited with status 1",
                new BackendId("Ollama glm4:9b"),
                new CommandResult(1, "", "model error", Duration.ofMillis(1), false)
        );

        String message = DesktopErrorFormatter.messageFor(exception);

        assertTrue(message.contains("Ollama glm4:9b exited with status 1"));
        assertTrue(message.contains("stderr:\nmodel error"));
    }

    @Test
    void backendErrorDoesNotDuplicateStderrAlreadyInMessage() {
        AiBackendException exception = new AiBackendExecutionException(
                "Ollama glm4:9b exited with status 1: model error",
                new BackendId("Ollama glm4:9b"),
                new CommandResult(1, "", "model error", Duration.ofMillis(1), false)
        );

        String message = DesktopErrorFormatter.messageFor(exception);

        assertEquals(1, occurrencesOf(message, "model error"));
        assertFalse(message.contains("stderr:"));
    }

    @Test
    void plainRuntimeErrorUsesMessage() {
        assertEquals("plain failure", DesktopErrorFormatter.messageFor(new RuntimeException("plain failure")));
    }

    @Test
    void backendChoiceDisplaysLabel() {
        assertEquals("GLM", new BackendChoice("glm", "GLM").toString());
    }

    private static int occurrencesOf(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) >= 0) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
