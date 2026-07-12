package myclaw;

final class AiBackendStartupException extends AiBackendException {
    AiBackendStartupException(String message, BackendId backendId, Throwable cause) {
        super(message, backendId, cause);
    }
}
