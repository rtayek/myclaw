package myclaw.application;

public final class TranscriptIngestionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TranscriptIngestionException(String message) {
        super(message);
    }

    public TranscriptIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
