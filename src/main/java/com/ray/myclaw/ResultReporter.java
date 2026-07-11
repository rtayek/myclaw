package com.ray.myclaw;

import java.io.PrintStream;
import java.util.Objects;

final class ResultReporter {
    private final PrintStream out;
    private final PrintStream err;

    ResultReporter(PrintStream out, PrintStream err) {
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    void reportSuccess(AiResponse response) {
        out.print(response.text());
    }

    void reportFailure(AiBackendException exception) {
        err.println(exception.getMessage());
        exception.commandResult()
                .map(CommandResult::standardError)
                .filter(stderr -> !stderr.isBlank())
                .ifPresent(err::print);
    }

    void reportUsageError(String message) {
        err.println(message);
    }

    void reportTranscriptWriteFailure(TranscriptWriteException exception) {
        err.println(exception.getMessage());
    }
}
