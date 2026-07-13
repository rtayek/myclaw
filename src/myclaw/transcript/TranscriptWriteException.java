package myclaw.transcript;

public final class TranscriptWriteException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TranscriptWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
