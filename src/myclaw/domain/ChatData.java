package myclaw.domain;

import java.util.Objects;

/**
 * Record representing metadata for a chat session.
 *
 * @param id          Unique identifier for the chat
 * @param projectName Name of the project the chat belongs to
 * @param chatId      Backend or URL chat identifier (e.g. UUID in chatgpt.com/c/<chatId>)
 * @param title       Title of the chat
 * @param webUrl      Full web URL of the chat
 * @param provider    LLM backend / provider (e.g. chatgpt, claude, glm)
 * @param lastActive  ISO timestamp or description of last activity
 */
public record ChatData(
        String id,
        String projectName,
        String chatId,
        String title,
        String webUrl,
        String provider,
        String lastActive
) {
    public ChatData {
        id = (id != null && !id.isBlank()) ? id : (chatId != null && !chatId.isBlank() ? chatId : "");
        projectName = projectName != null ? projectName : "";
        chatId = chatId != null ? chatId : "";
        title = title != null ? title : "";
        webUrl = webUrl != null ? webUrl : "";
        provider = (provider != null && !provider.isBlank()) ? provider : "chatgpt";
        lastActive = lastActive != null ? lastActive : "";
    }

    public static ChatData create(
            String projectName,
            String title,
            String webUrl,
            String provider,
            String lastActive
    ) {
        String extractedChatId = extractChatId(webUrl);
        String id = (extractedChatId != null && !extractedChatId.isBlank())
                ? extractedChatId
                : (title != null ? title.replaceAll("[^a-zA-Z0-9_-]", "-") : "chat-1");
        return new ChatData(id, projectName, extractedChatId, title, webUrl, provider, lastActive);
    }

    public static String extractChatId(String webUrl) {
        if (webUrl == null || webUrl.isBlank()) {
            return "";
        }
        int cIndex = webUrl.indexOf("/c/");
        if (cIndex >= 0) {
            String sub = webUrl.substring(cIndex + 3);
            int slashIndex = sub.indexOf('/');
            return (slashIndex > 0) ? sub.substring(0, slashIndex) : sub;
        }
        int gIndex = webUrl.indexOf("/g/");
        if (gIndex >= 0) {
            String sub = webUrl.substring(gIndex + 3);
            int cSubIndex = sub.indexOf("/c/");
            if (cSubIndex >= 0) {
                String chatPart = sub.substring(cSubIndex + 3);
                int slashIndex = chatPart.indexOf('/');
                return (slashIndex > 0) ? chatPart.substring(0, slashIndex) : chatPart;
            }
        }
        int lastSlash = webUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < webUrl.length() - 1) {
            return webUrl.substring(lastSlash + 1);
        }
        return "";
    }
}
