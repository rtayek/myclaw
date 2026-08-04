package myclaw.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ChatDataTest {

    @Test
    void chatDataStoresAllSevenMetadataFields() {
        ChatData data = new ChatData(
                "chat-123",
                "MyClaw",
                "chat-123",
                "Architecture Chat",
                "https://chatgpt.com/c/chat-123",
                "chatgpt",
                "2026-08-04T11:00:00Z"
        );

        assertEquals("chat-123", data.id());
        assertEquals("MyClaw", data.projectName());
        assertEquals("chat-123", data.chatId());
        assertEquals("Architecture Chat", data.title());
        assertEquals("https://chatgpt.com/c/chat-123", data.webUrl());
        assertEquals("chatgpt", data.provider());
        assertEquals("2026-08-04T11:00:00Z", data.lastActive());
    }

    @Test
    void extractChatIdParsesChatGptUrls() {
        assertEquals("6a62806d-643c-83e8-96cd-aa4e66e5d56d", ChatData.extractChatId("https://chatgpt.com/c/6a62806d-643c-83e8-96cd-aa4e66e5d56d"));
        assertEquals("custom-gpt-123", ChatData.extractChatId("https://chatgpt.com/g/g-123/c/custom-gpt-123"));
        assertEquals("", ChatData.extractChatId(null));
    }
}
