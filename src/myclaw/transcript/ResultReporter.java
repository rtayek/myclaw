package myclaw.transcript;

import java.io.PrintStream;
import java.util.Objects;

import myclaw.application.PromptResult;
import myclaw.backend.AiBackendException;
import myclaw.execution.CommandResult;

public final class ResultReporter {
    private final PrintStream out;
    private final PrintStream err;

    public ResultReporter(PrintStream out, PrintStream err) {
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    public void reportSuccess(PromptResult result) {
        out.print(result.response());
    }

    public void reportFailure(AiBackendException exception) {
        err.println(exception.getMessage());
        exception.commandResult()
                .map(CommandResult::standardError)
                .filter(stderr -> !stderr.isBlank())
                .filter(stderr -> !exception.getMessage().contains(stderr.strip()))
                .ifPresent(err::print);
    }

    public void reportUsageError(String message) {
        err.println(message);
    }

    public void reportTranscriptWriteFailure(TranscriptWriteException exception) {
        err.println(exception.getMessage());
    }
}
