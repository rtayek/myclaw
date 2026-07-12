package myclaw.backend;

public final class AiBackendStartupException extends AiBackendException {
    public AiBackendStartupException(String message, BackendId backendId, Throwable cause) {
        super(message, backendId, cause);
    }
}
