package myclaw.transport.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.List;

import myclaw.application.BackendDescriptor;
import myclaw.application.PromptResult;
import myclaw.application.PromptService;
import myclaw.backend.AiBackendExecutionException;
import myclaw.backend.AiBackendStartupException;
import myclaw.backend.AiBackendUnsupportedRequestException;
import myclaw.backend.PromptProfile;

final class SocketJsonProtocol {
    static final int PROTOCOL_VERSION = 1;

    private final PromptService promptService;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    SocketJsonProtocol(PromptService promptService) {
        this.promptService = promptService;
    }

    ProtocolResult handle(String frame) {
        JsonObject request;
        try {
            JsonElement parsed = JsonParser.parseString(frame);
            if (!parsed.isJsonObject()) {
                return ProtocolResult.keepOpen(error(null, "validation_error", "The request must be a JSON object."));
            }
            request = parsed.getAsJsonObject();
        } catch (JsonParseException exception) {
            return ProtocolResult.closeAfter(error(null, "malformed_json", "The request is not valid JSON."));
        }

        String requestId = stringField(request, "requestId");
        if (requestId == null || requestId.isBlank()) {
            return ProtocolResult.keepOpen(error(requestId, "validation_error", "requestId is required and must be a nonblank string."));
        }

        String operation = stringField(request, "operation");
        if (operation == null || operation.isBlank()) {
            return ProtocolResult.keepOpen(error(requestId, "validation_error", "operation is required and must be a nonblank string."));
        }

        return switch (operation) {
            case "health" -> ProtocolResult.keepOpen(health(requestId));
            case "listBackends" -> ProtocolResult.keepOpen(listBackends(requestId));
            case "chat" -> ProtocolResult.keepOpen(chat(requestId, request));
            default -> ProtocolResult.keepOpen(error(requestId, "unknown_operation", "Unknown operation '" + operation + "'."));
        };
    }

    String requestTooLarge() {
        return error(null, "request_too_large", "The request frame exceeds the configured maximum size.");
    }

    private String health(String requestId) {
        JsonObject response = ok(requestId);
        response.addProperty("protocolVersion", PROTOCOL_VERSION);
        return gson.toJson(response);
    }

    private String listBackends(String requestId) {
        JsonObject response = ok(requestId);
        List<JsonObject> backends = promptService.backends().stream()
                .map(SocketJsonProtocol::backendJson)
                .toList();
        response.add("backends", gson.toJsonTree(backends));
        return gson.toJson(response);
    }

    private String chat(String requestId, JsonObject request) {
        String backendId = stringField(request, "backendId");
        if (backendId == null || backendId.isBlank()) {
            return error(requestId, "validation_error", "backendId is required and must be a nonblank string.");
        }

        String prompt = stringField(request, "prompt");
        if (prompt == null || prompt.isBlank()) {
            return error(requestId, "validation_error", "prompt is required and must be a nonblank string.");
        }

        PromptProfile profile;
        try {
            String profileName = stringField(request, "profile");
            profile = profileName == null ? PromptProfile.GENERAL : PromptProfile.fromExternalName(profileName);
        } catch (IllegalArgumentException exception) {
            return error(requestId, "validation_error", exception.getMessage());
        }

        if (!promptService.hasBackend(backendId)) {
            return error(requestId, "unknown_backend", "No backend is registered with id '" + backendId + "'.");
        }

        try {
            PromptResult result = promptService.submit(backendId, prompt, profile);
            JsonObject response = ok(requestId);
            response.addProperty("backendId", backendId);
            response.addProperty("content", result.response());
            return gson.toJson(response);
        } catch (AiBackendStartupException exception) {
            return error(requestId, "backend_unavailable", "The backend is unavailable.");
        } catch (AiBackendExecutionException exception) {
            return error(requestId, "backend_failed", "The backend failed while processing the request.");
        } catch (AiBackendUnsupportedRequestException exception) {
            return error(requestId, "unsupported_request", "The backend does not support this request.");
        } catch (RuntimeException exception) {
            SocketTransportLog.error("Unhandled socket chat request failure.", exception);
            return error(requestId, "internal_error", "An internal error occurred.");
        }
    }

    private static JsonObject backendJson(BackendDescriptor descriptor) {
        JsonObject object = new JsonObject();
        object.addProperty("id", descriptor.id());
        object.addProperty("label", descriptor.label());
        return object;
    }

    private static JsonObject ok(String requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("requestId", requestId);
        response.addProperty("status", "ok");
        return response;
    }

    private String error(String requestId, String code, String message) {
        JsonObject response = new JsonObject();
        if (requestId == null) {
            response.add("requestId", JsonNull.INSTANCE);
        } else {
            response.addProperty("requestId", requestId);
        }
        response.addProperty("status", "error");

        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return gson.toJson(response);
    }

    private static String stringField(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return null;
        }
        return value.getAsString();
    }

    record ProtocolResult(String response, boolean closeConnection) {
        static ProtocolResult keepOpen(String response) {
            return new ProtocolResult(response, false);
        }

        static ProtocolResult closeAfter(String response) {
            return new ProtocolResult(response, true);
        }
    }
}
