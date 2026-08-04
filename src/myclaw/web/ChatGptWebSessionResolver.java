package myclaw.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves ChatGPT web chat titles to their direct URLs by running chatgpt-web-sessions.sh.
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

        return parseOutput(output.toString());
    }

    public static Map<String, String> parseOutput(String output) {
        Map<String, String> sessions = new LinkedHashMap<>();
        if (output == null || output.isBlank()) {
            return sessions;
        }

        String[] lines = output.split("\r?\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("Warning:") || line.startsWith("No ChatGPT")) {
                continue;
            }
            int arrowIndex = line.indexOf(" -> ");
            if (arrowIndex > 0) {
                String title = line.substring(0, arrowIndex).trim();
                String url = line.substring(arrowIndex + 4).trim();
                if (!title.isEmpty() && !url.isEmpty()) {
                    sessions.put(title, url);
                }
            }
        }
        return sessions;
    }
}
