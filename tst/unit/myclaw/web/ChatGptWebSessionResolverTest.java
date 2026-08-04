package myclaw.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChatGptWebSessionResolverTest {

    @Test
    void defaultScriptPathIsLocalScript() {
        ChatGptWebSessionResolver resolver = new ChatGptWebSessionResolver();
        assertEquals("./chatgpt-web-sessions.sh", resolver.scriptPath());
    }

    @Test
    void parseOutputParsesTitleAndUrlPairs() {
        String sampleOutput = """
                Warning: Could not connect to Chrome remote debugging port at http://127.0.0.1:9222.
                MyClaw Architecture Chat -> https://chatgpt.com/c/123-abc
                Documentation Consolidation -> https://chatgpt.com/c/456-def
                """;

        Map<String, String> result = ChatGptWebSessionResolver.parseOutput(sampleOutput);

        assertEquals(2, result.size());
        assertEquals("https://chatgpt.com/c/123-abc", result.get("MyClaw Architecture Chat"));
        assertEquals("https://chatgpt.com/c/456-def", result.get("Documentation Consolidation"));
    }

    @Test
    void parseOutputHandlesEmptyOrNullInput() {
        assertTrue(ChatGptWebSessionResolver.parseOutput(null).isEmpty());
        assertTrue(ChatGptWebSessionResolver.parseOutput("").isEmpty());
        assertTrue(ChatGptWebSessionResolver.parseOutput("No ChatGPT web chats found.").isEmpty());
    }
}
