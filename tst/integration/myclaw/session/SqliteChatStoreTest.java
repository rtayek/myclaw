package myclaw.session;

import myclaw.domain.ChatData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqliteChatStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndRetrievesChatDataAndProjects() {
        Path dbPath = tempDir.resolve("chats.db");
        try (SqliteChatStore store = new SqliteChatStore(dbPath)) {
            ChatData chat = new ChatData(
                    "c-1",
                    "MyClaw",
                    "c-1",
                    "Refactoring Pipeline",
                    "https://chatgpt.com/c/c-1",
                    "chatgpt",
                    "2026-08-04T11:30:00Z"
            );

            store.saveChat(chat);

            Optional<ChatData> retrieved = store.getChat("c-1");
            assertTrue(retrieved.isPresent());
            assertEquals("c-1", retrieved.get().id());
            assertEquals("MyClaw", retrieved.get().projectName());
            assertEquals("Refactoring Pipeline", retrieved.get().title());
            assertEquals("https://chatgpt.com/c/c-1", retrieved.get().webUrl());
            assertEquals("chatgpt", retrieved.get().provider());
            assertEquals("2026-08-04T11:30:00Z", retrieved.get().lastActive());

            List<String> projects = store.listProjects();
            assertEquals(List.of("MyClaw"), projects);

            List<ChatData> projectChats = store.listChatsByProject("MyClaw");
            assertEquals(1, projectChats.size());
            assertEquals("c-1", projectChats.get(0).id());
        }
    }
}
