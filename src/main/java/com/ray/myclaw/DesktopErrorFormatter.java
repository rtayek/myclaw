package com.ray.myclaw;

final class DesktopErrorFormatter {
    private DesktopErrorFormatter() {
    }

    static String messageFor(Throwable throwable) {
        if (throwable instanceof AiBackendException exception) {
            StringBuilder message = new StringBuilder(exception.getMessage());
            exception.commandResult()
                    .map(CommandResult::standardError)
                    .filter(stderr -> !stderr.isBlank())
                    .filter(stderr -> !exception.getMessage().contains(stderr.strip()))
                    .ifPresent(stderr -> message.append("\n\nstderr:\n").append(stderr));
            return message.toString();
        }
        if (throwable instanceof TranscriptWriteException) {
            return throwable.getMessage();
        }
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }
}
