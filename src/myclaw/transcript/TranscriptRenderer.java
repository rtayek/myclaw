package myclaw.transcript;

import myclaw.execution.CommandResult;

final class TranscriptRenderer {
    private TranscriptRenderer() {
    }

    static String render(Transcript transcript) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# AI Run\n\n");
        markdown.append("Run ID: ").append(transcript.runId()).append("\n");
        markdown.append("Backend: ").append(transcript.backendId()).append("\n");
        markdown.append("Started: ").append(transcript.started()).append("\n");
        markdown.append("Duration: ").append(transcript.duration()).append("\n");
        transcript.commandResult().ifPresent(commandResult -> {
            markdown.append("Exit code: ").append(commandResult.exitCode()).append("\n");
            markdown.append("Timed out: ").append(commandResult.timedOut()).append("\n");
        });
        transcript.errorMessage().ifPresent(errorMessage -> markdown.append("Error: ").append(errorMessage).append("\n"));

        appendSection(markdown, "Prompt", transcript.request().prompt());
        appendSection(markdown, "Response", transcript.responseText());
        appendSection(markdown, "Command", String.join("\n", transcript.command()));
        appendSection(markdown, "Standard Error", transcript.commandResult()
                .map(CommandResult::standardError)
                .orElse(""));
        return markdown.toString();
    }

    private static void appendSection(StringBuilder markdown, String heading, String content) {
        markdown.append("\n## ").append(heading).append("\n\n");
        String fence = fenceFor(content);
        markdown.append(fence).append("\n");
        markdown.append(content);
        if (!content.endsWith("\n")) {
            markdown.append("\n");
        }
        markdown.append(fence).append("\n");
    }

    private static String fenceFor(String content) {
        int longestRun = 0;
        int currentRun = 0;
        for (int index = 0; index < content.length(); index++) {
            if (content.charAt(index) == '`') {
                currentRun++;
                longestRun = Math.max(longestRun, currentRun);
            } else {
                currentRun = 0;
            }
        }
        return "`".repeat(Math.max(3, longestRun + 1));
    }
}
