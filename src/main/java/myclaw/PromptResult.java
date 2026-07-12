package myclaw;

import java.nio.file.Path;
import java.util.Objects;

record PromptResult(
        String backendLabel,
        String response,
        Path transcriptPath
) {
    PromptResult {
        Objects.requireNonNull(backendLabel, "backendLabel");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(transcriptPath, "transcriptPath");
    }
}
