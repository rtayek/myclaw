package myclaw.session;

public final class SessionStoreException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SessionStoreException(String message) {
        super(message);
    }

    public SessionStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
