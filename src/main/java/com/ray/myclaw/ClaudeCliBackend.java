package com.ray.myclaw;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

final class ClaudeCliBackend implements AiBackend {
    static final String BACKEND_NAME = "Claude CLI";

    private final CommandExecutor commandExecutor;
    private final Duration timeout;

    ClaudeCliBackend(CommandExecutor commandExecutor, Duration timeout) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public AiResponse ask(AiRequest request) {
        return askWithResult(request).response();
    }

    ClaudeCliRun askWithResult(AiRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.systemPrompt().isPresent()) {
            throw new AiBackendException("Claude CLI backend does not support system prompts yet.", BACKEND_NAME, (CommandResult) null);
        }

        List<String> command = List.of("claude", "-p", request.prompt());
        CommandResult result;
        try {
            result = commandExecutor.run(new CommandRequest(command, "", timeout));
        } catch (CommandExecutionException exception) {
            throw new AiBackendException("Could not start Claude CLI: " + exception.getMessage(), BACKEND_NAME, exception);
        }

        if (result.timedOut()) {
            throw new AiBackendException("Claude CLI timed out after " + timeout, BACKEND_NAME, result);
        }
        if (result.exitCode() != 0) {
            throw new AiBackendException(nonzeroExitMessage(result), BACKEND_NAME, result);
        }

        AiResponse response = new AiResponse(
                result.standardOutput(),
                BACKEND_NAME,
                result.duration()
        );
        return new ClaudeCliRun(response, result, command);
    }

    private static String nonzeroExitMessage(CommandResult result) {
        String message = "Claude CLI exited with status " + result.exitCode();
        if (!result.standardError().isBlank()) {
            return message + ": " + result.standardError().strip();
        }
        return message;
    }
}
