package myclaw.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import myclaw.domain.ChatData;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves ChatGPT web chat titles and metadata to ChatData records by running chatgpt-web-sessions.sh.
 */
public class ChatGptWebSessionResolver {
    public static final String DEFAULT_SCRIPT_PATH = "./chatgpt-web-sessions.sh";

    private final String scriptPath;

    public ChatGptWebSessionResolver() {
        this(DEFAULT_SCRIPT_PATH);
    }

    public ChatGptWebSessionResolver(Path scriptPath) {
        this(Objects.requireNonNull(scriptPath, "scriptPath").toString());
    }

    public ChatGptWebSessionResolver(String scriptPath) {
        this.scriptPath = Objects.requireNonNull(scriptPath, "scriptPath");
    }

    public String scriptPath() {
        return scriptPath;
    }

    public Map<String, String> resolveSessions() {
        return resolveSessions(null);
    }

    public Map<String, String> resolveSessions(String cdpUrl) {
        return parseOutput(runScript(cdpUrl));
    }

    public List<ChatData> resolveChatData() {
        return resolveChatData(null, "Default");
    }

    public List<ChatData> resolveChatData(String cdpUrl, String defaultProjectName) {
        return parseChatDataList(runScript(cdpUrl), defaultProjectName);
    }

    private String runScript(String cdpUrl) {
        ProcessBuilder pb;
        if (cdpUrl != null && !cdpUrl.isBlank()) {
            pb = new ProcessBuilder("sh", scriptPath, cdpUrl);
        } else {
            pb = new ProcessBuilder("sh", scriptPath);
        }
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to execute chatgpt-web-sessions script: " + scriptPath, exception);
        }
        return output.toString();
    }

    public static Map<String, String> parseOutput(String output) {
        Map<String, String> sessions = new LinkedHashMap<>();
        for (ChatData chat : parseChatDataList(output, "Default")) {
            sessions.put(chat.title(), chat.webUrl());
        }
        return sessions;
    }

    public static List<ChatData> parseChatDataList(String output, String defaultProject) {
        List<ChatData> chats = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return chats;
        }

        String fallbackProject = (defaultProject != null && !defaultProject.isBlank()) ? defaultProject : "Default";

        String[] lines = output.split("\r?\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("Warning:") || line.startsWith("No ChatGPT")) {
                continue;
            }

            int arrowIndex = line.indexOf(" -> ");
            if (arrowIndex <= 0) {
                continue;
            }

            String leftPart = line.substring(0, arrowIndex).trim();
            String rightPart = line.substring(arrowIndex + 4).trim();

            String projectName = fallbackProject;
            String title = leftPart;
            String webUrl = rightPart;
            String lastActive = "";

            // Parse [ProjectName] prefix if present in title
            if (title.startsWith("[")) {
                int closingBracket = title.indexOf("]");
                if (closingBracket > 1) {
                    projectName = title.substring(1, closingBracket).trim();
                    title = title.substring(closingBracket + 1).trim();
                }
            } else if (title.contains(" | ")) {
                String[] parts = title.split("\\s*\\|\\s*", 2);
                projectName = parts[0].trim();
                title = parts[1].trim();
            }

            // Parse | lastActive timestamp suffix if present in webUrl
            if (webUrl.contains(" | ")) {
                String[] parts = webUrl.split("\\s*\\|\\s*", 2);
                webUrl = parts[0].trim();
                lastActive = parts[1].trim();
            }

            String chatId = ChatData.extractChatId(webUrl);
            String id = !chatId.isBlank() ? chatId : title.replaceAll("[^a-zA-Z0-9_-]", "-");

            chats.add(new ChatData(id, projectName, chatId, title, webUrl, "chatgpt", lastActive));
        }
        return chats;
    }
}
