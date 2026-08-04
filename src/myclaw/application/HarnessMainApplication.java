package myclaw.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import myclaw.backend.AiBackendException;
import myclaw.transcript.ResultReporter;
import myclaw.transcript.TranscriptWriteException;

import java.nio.file.Path;
import java.util.Objects;

import myclaw.backend.AiBackendException;
import myclaw.transcript.ResultReporter;
import myclaw.transcript.TranscriptWriteException;

public final class HarnessMainApplication {
    private static final String USAGE = "Usage: java -jar ai-harness.jar <backend> \"prompt\" | ingest <chat-file-path> [projectName] [backend]";

    private final PromptService promptService;
    private final TranscriptIngestionService ingestionService;
    private final ResultReporter reporter;
    private final InputStream input;

    public HarnessMainApplication(
            PromptService promptService,
            ResultReporter reporter,
            InputStream input
    ) {
        this(promptService, new TranscriptIngestionService(promptService), reporter, input);
    }

    public HarnessMainApplication(
            PromptService promptService,
            TranscriptIngestionService ingestionService,
            ResultReporter reporter,
            InputStream input
    ) {
        this.promptService = Objects.requireNonNull(promptService, "promptService");
        this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.input = Objects.requireNonNull(input, "input");
    }

    public int run(String[] args) {
        if (args.length == 0) {
            reporter.reportUsageError(USAGE);
            return 2;
        }

        if ("ingest".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                reporter.reportUsageError(USAGE);
                return 2;
            }
            Path inputPath = Path.of(args[1]);
            String projectName = null;
            String backendName = "claude";

            if (args.length == 3) {
                if (promptService.hasBackend(args[2])) {
                    backendName = args[2];
                } else {
                    projectName = args[2];
                }
            } else if (args.length >= 4) {
                projectName = args[2];
                backendName = args[3];
            }

            if (!promptService.hasBackend(backendName)) {
                reporter.reportUsageError("Unknown backend: " + backendName);
                return 2;
            }
            try {
                Path outputPath = ingestionService.ingest(inputPath, backendName, projectName);
                reporter.reportIngestSuccess(outputPath);
                return 0;
            } catch (AiBackendException exception) {
                reporter.reportFailure(exception);
                return 1;
            } catch (TranscriptWriteException exception) {
                reporter.reportTranscriptWriteFailure(exception);
                return 1;
            } catch (TranscriptIngestionException | IllegalArgumentException exception) {
                reporter.reportUsageError(exception.getMessage());
                return 1;
            }
        }

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
    private static final long serialVersionUID = 1L;

    PromptInputException(String message) {
        super(message);
    }

    PromptInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
