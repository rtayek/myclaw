package myclaw.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public final class TranscriptIngestionService {
    private final PromptService promptService;

    public TranscriptIngestionService(PromptService promptService) {
        this.promptService = Objects.requireNonNull(promptService, "promptService");
    }

    public Path ingest(Path inputFilePath) {
        return ingest(inputFilePath, "claude", null);
    }

    public Path ingest(Path inputFilePath, String backendName) {
        return ingest(inputFilePath, backendName, null);
    }

    public Path ingest(Path inputFilePath, String backendName, String projectName) {
        Objects.requireNonNull(inputFilePath, "inputFilePath");
        Objects.requireNonNull(backendName, "backendName");

        if (!Files.exists(inputFilePath)) {
            throw new IllegalArgumentException("Chat transcript file does not exist: " + inputFilePath);
        }

        String rawContent;
        try {
            rawContent = Files.readString(inputFilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TranscriptIngestionException("Failed to read chat transcript file: " + inputFilePath, e);
        }

        String extractedMessages = extractMessages(inputFilePath, rawContent);
        String prompt = wrapInSummarizationPrompt(extractedMessages);

        PromptResult result = promptService.submit(backendName, prompt);

        Path outputPath = computeOutputPath(inputFilePath, projectName);
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, result.response(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TranscriptIngestionException("Failed to write consolidated summary to: " + outputPath, e);
        }

        return outputPath;
    }

    public static Path computeOutputPath(Path inputFilePath) {
        return computeOutputPath(inputFilePath, null);
    }

    public static Path computeOutputPath(Path inputFilePath, String projectName) {
        Objects.requireNonNull(inputFilePath, "inputFilePath");
        if (projectName != null && !projectName.isBlank()) {
            return Path.of(projectName + "-CONSOLIDATED.md");
        }
        Path fileNamePath = inputFilePath.getFileName();
        String fileName = fileNamePath == null ? "PROJECT" : fileNamePath.toString();
        int lastDot = fileName.lastIndexOf('.');
        String baseName = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
        return inputFilePath.resolveSibling(baseName + "-CONSOLIDATED.md");
    }

    public static String extractMessages(Path inputFilePath, String rawContent) {
        Objects.requireNonNull(inputFilePath, "inputFilePath");
        Objects.requireNonNull(rawContent, "rawContent");

        Path fileNamePath = inputFilePath.getFileName();
        String fileName = (fileNamePath != null) ? fileNamePath.toString().toLowerCase() : "";
        String trimmed = rawContent.trim();
        boolean isJson = fileName.endsWith(".json") || trimmed.startsWith("{") || trimmed.startsWith("[");

        if (isJson && !trimmed.isEmpty()) {
            try {
                JsonElement element = JsonParser.parseString(trimmed);
                StringBuilder sb = new StringBuilder();
                if (element.isJsonArray()) {
                    appendJsonArrayMessages(element.getAsJsonArray(), sb);
                } else if (element.isJsonObject()) {
                    appendJsonObjectMessages(element.getAsJsonObject(), sb);
                }
                String extracted = sb.toString().trim();
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            } catch (JsonSyntaxException ignored) {
                // Fall back to raw text
            }
        }
        return trimmed;
    }

    public static String wrapInSummarizationPrompt(String content) {
        return """
                You are an expert AI software architect consolidating project progress from a chat transcript.%n\
                %n\
                Please analyze the chat transcript provided below and generate a structured, consolidated project summary document (<Project>-CONSOLIDATED.md).%n\
                The summary should include:%n\
                1. Target Objective & High-level Overview%n\
                2. Key Architectural Decisions & Components%n\
                3. Implementation Details & Key Findings%n\
                4. Current Status & Next Action Steps%n\
                %n\
                Chat Transcript:%n\
                ---%n\
                %s%n\
                ---%n\
                """.formatted(content);
    }

    private static void appendJsonArrayMessages(JsonArray array, StringBuilder sb) {
        for (JsonElement item : array) {
            if (item.isJsonObject()) {
                appendJsonObjectMessage(item.getAsJsonObject(), sb);
            } else if (item.isJsonPrimitive()) {
                sb.append(item.getAsString()).append("\n\n");
            }
        }
    }

    private static void appendJsonObjectMessages(JsonObject obj, StringBuilder sb) {
        if (obj.has("messages") && obj.get("messages").isJsonArray()) {
            appendJsonArrayMessages(obj.getAsJsonArray("messages"), sb);
        } else if (obj.has("transcript") && obj.get("transcript").isJsonArray()) {
            appendJsonArrayMessages(obj.getAsJsonArray("transcript"), sb);
        } else {
            appendJsonObjectMessage(obj, sb);
        }
    }

    private static void appendJsonObjectMessage(JsonObject obj, StringBuilder sb) {
        if (obj.has("role") && (obj.has("content") || obj.has("text"))) {
            String role = obj.get("role").getAsString();
            String content = obj.has("content") ? obj.get("content").getAsString() : obj.get("text").getAsString();
            sb.append(role.toUpperCase()).append(": ").append(content).append("\n\n");
        } else if (obj.has("prompt") && (obj.has("response") || obj.has("responseText"))) {
            String prompt = obj.get("prompt").getAsString();
            String response = obj.has("response") ? obj.get("response").getAsString() : obj.get("responseText").getAsString();
            sb.append("USER: ").append(prompt).append("\n\nASSISTANT: ").append(response).append("\n\n");
        } else if (obj.has("content")) {
            sb.append(obj.get("content").getAsString()).append("\n\n");
        } else if (obj.has("text")) {
            sb.append(obj.get("text").getAsString()).append("\n\n");
        }
    }
}
