package myclaw.backend;

public final class AiBackendUnsupportedRequestException extends AiBackendException {
    public AiBackendUnsupportedRequestException(String message, BackendId backendId) {
        super(message, backendId);
    }
}
