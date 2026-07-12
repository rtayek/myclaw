package myclaw;

final class AiBackendUnsupportedRequestException extends AiBackendException {
    AiBackendUnsupportedRequestException(String message, BackendId backendId) {
        super(message, backendId);
    }
}
