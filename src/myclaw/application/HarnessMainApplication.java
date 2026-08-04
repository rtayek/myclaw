package myclaw.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import myclaw.backend.AiBackendException;
import myclaw.transcript.ResultReporter;
import myclaw.transcript.TranscriptWriteException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import myclaw.backend.AiBackendException;
import myclaw.transcript.ResultReporter;
import myclaw.transcript.TranscriptWriteException;

import myclaw.web.PlaywrightWebAdapter;

public final class HarnessMainApplication {
    private static final String USAGE = "Usage: java -jar ai-harness.jar <backend> \"prompt\" | ingest <chat-file-path> [projectName] [backend] | sessions [backend] | submit [--session <session-id>] --prompt \"prompt\" [backend] | web-sessions [cdpUrl] | web-submit [--url <chatUrl> | --title <chatTitle>] --prompt \"prompt\" [projectName] [cdpUrl]";

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

        if ("web-sessions".equalsIgnoreCase(args[0]) || "chatgpt-web".equalsIgnoreCase(args[0])) {
            String cdpUrl = (args.length >= 2) ? args[1] : PlaywrightWebAdapter.DEFAULT_CDP_URL;
            try (PlaywrightWebAdapter adapter = new PlaywrightWebAdapter(cdpUrl)) {
                var chats = adapter.listChatGPTChats();
                if (!adapter.isConnectedViaCdp()) {
                    System.err.println("Warning: Could not connect to Chrome remote debugging port at " + cdpUrl + ".");
                    System.err.println("  (Launched a new browser instance. For logged-in sessions, launch Chrome with: chrome --remote-debugging-port=9222)");
                }
                if (chats.isEmpty()) {
                    System.out.println("No ChatGPT web chats found.");
                } else {
                    for (var chat : chats) {
                        System.out.println(chat.title() + " -> " + chat.url());
                    }
                }
                return 0;
            } catch (Exception exception) {
                reporter.reportUsageError("Could not list ChatGPT web chats: " + exception.getMessage());
                return 1;
            }
        }

        if ("web-submit".equalsIgnoreCase(args[0]) || "chatgpt-web-submit".equalsIgnoreCase(args[0])) {
            String chatUrl = null;
            String chatTitle = null;
            String prompt = null;
            String projectName = null;
            String cdpUrl = PlaywrightWebAdapter.DEFAULT_CDP_URL;

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("--url".equalsIgnoreCase(arg) || "-u".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) chatUrl = args[++i];
                } else if ("--title".equalsIgnoreCase(arg) || "-t".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) chatTitle = args[++i];
                } else if ("--prompt".equalsIgnoreCase(arg) || "-p".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) prompt = args[++i];
                } else if ("--project".equalsIgnoreCase(arg) || "-pr".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) projectName = args[++i];
                } else if ("--cdp".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) cdpUrl = args[++i];
                } else if (arg.startsWith("http://127.0.0.1") || arg.startsWith("http://localhost")) {
                    cdpUrl = arg;
                } else if (chatUrl == null && (arg.contains("/c/") || arg.startsWith("http"))) {
                    chatUrl = arg;
                } else if (prompt == null && !arg.startsWith("-")) {
                    prompt = arg;
                }
            }

            if (prompt == null || prompt.isBlank()) {
                try {
                    prompt = promptFrom("-", input);
                } catch (PromptInputException exception) {
                    reporter.reportUsageError("Prompt is required for web-submit command.");
                    return 2;
                }
            }

            try (PlaywrightWebAdapter adapter = new PlaywrightWebAdapter(cdpUrl)) {
                String responseText;
                if (chatUrl != null && !chatUrl.isBlank()) {
                    responseText = adapter.submitPrompt(chatUrl, prompt);
                } else if (chatTitle != null && !chatTitle.isBlank()) {
                    responseText = adapter.submitToTitle(chatTitle, prompt);
                } else {
                    responseText = adapter.submitPrompt("https://chatgpt.com", prompt);
                }

                System.out.println(responseText);

                if (projectName != null && !projectName.isBlank()) {
                    Path outputPath = Path.of(projectName + "-CONSOLIDATED.md");
                    try {
                        Files.writeString(outputPath, responseText, StandardCharsets.UTF_8);
                        reporter.reportIngestSuccess(outputPath);
                    } catch (IOException e) {
                        reporter.reportUsageError("Could not write consolidated output to: " + outputPath);
                    }
                }
                return 0;
            } catch (Exception exception) {
                reporter.reportUsageError("Could not submit web prompt: " + exception.getMessage());
                return 1;
            }
        }

        if ("sessions".equalsIgnoreCase(args[0])) {
            String backendName = (args.length >= 2) ? args[1] : "claude";
            if (!promptService.hasBackend(backendName)) {
                reporter.reportUsageError("Unknown backend: " + backendName);
                return 2;
            }
            try {
                reporter.reportSessions(promptService.listSessions(backendName));
                return 0;
            } catch (Exception exception) {
                reporter.reportUsageError(exception.getMessage());
                return 1;
            }
        }

        if ("submit".equalsIgnoreCase(args[0])) {
            String sessionId = null;
            String prompt = null;
            String backendName = "claude";

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("--session".equalsIgnoreCase(arg) || "-s".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) {
                        sessionId = args[++i];
                    }
                } else if ("--prompt".equalsIgnoreCase(arg) || "-p".equalsIgnoreCase(arg)) {
                    if (i + 1 < args.length) {
                        prompt = args[++i];
                    }
                } else if (!arg.startsWith("-") && promptService.hasBackend(arg)) {
                    backendName = arg;
                } else if (prompt == null && !arg.startsWith("-")) {
                    prompt = arg;
                }
            }

            if (prompt == null || prompt.isBlank()) {
                try {
                    prompt = promptFrom("-", input);
                } catch (PromptInputException exception) {
                    reporter.reportUsageError("Prompt is required for submit command.");
                    return 2;
                }
            }

            if (!promptService.hasBackend(backendName)) {
                reporter.reportUsageError("Unknown backend: " + backendName);
                return 2;
            }

            try {
                PromptResult result = promptService.submit(backendName, prompt, sessionId);
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
