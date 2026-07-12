package myclaw.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import myclaw.backend.AiBackendException;
import myclaw.transcript.ResultReporter;
import myclaw.transcript.TranscriptWriteException;

public final class HarnessMainApplication {
    private static final String USAGE = "Usage: java -jar ai-harness.jar <backend> \"prompt\"";

    private final PromptService promptService;
    private final ResultReporter reporter;
    private final InputStream input;

    public HarnessMainApplication(
            PromptService promptService,
            ResultReporter reporter,
            InputStream input
    ) {
        this.promptService = promptService;
        this.reporter = reporter;
        this.input = input;
    }

    public int run(String[] args) {
        if (args.length != 2) {
            reporter.reportUsageError(USAGE);
            return 2;
        }

        if (!promptService.hasBackend(args[0])) {
            reporter.reportUsageError(USAGE);
            return 2;
        }

        String prompt;
        try {
            prompt = promptFrom(args[1], input);
        } catch (PromptInputException exception) {
            reporter.reportUsageError(exception.getMessage());
            return 2;
        }

        try {
            PromptResult result = promptService.submit(args[0], prompt);
            reporter.reportSuccess(result);
            return 0;
        } catch (AiBackendException exception) {
            reporter.reportFailure(exception);
            return 1;
        } catch (TranscriptWriteException exception) {
            reporter.reportTranscriptWriteFailure(exception);
            return 1;
        }
    }

    private static String promptFrom(String argument, InputStream input) {
        if (!"-".equals(argument)) {
            return argument;
        }
        try {
            String prompt = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (prompt.isEmpty()) {
                throw new PromptInputException("Prompt from standard input is empty.");
            }
            return prompt;
        } catch (IOException exception) {
            throw new PromptInputException("Could not read prompt from standard input.", exception);
        }
    }
}

final class PromptInputException extends RuntimeException {
    PromptInputException(String message) {
        super(message);
    }

    PromptInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
