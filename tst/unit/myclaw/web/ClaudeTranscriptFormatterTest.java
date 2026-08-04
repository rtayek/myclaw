package myclaw.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClaudeTranscriptFormatterTest {

    @Test
    void formatsTurnsAsRolePrefixedMarkdownWithBlankLineBetween() {
        String md = ClaudeTranscriptFormatter.toRolePrefixedMarkdown(List.of(
                new ClaudeTurn("user", "How do I center a div?"),
                new ClaudeTurn("assistant", "Use flexbox.")));

        assertEquals("USER: How do I center a div?\n\nASSISTANT: Use flexbox.", md);
    }

    @Test
    void normalizesRoleSynonymsAndCase() {
        String md = ClaudeTranscriptFormatter.toRolePrefixedMarkdown(List.of(
                new ClaudeTurn("Human", "hi"),
                new ClaudeTurn("Claude", "hello")));

        assertEquals("USER: hi\n\nASSISTANT: hello", md);
    }

    @Test
    void dropsBlankTurnsSoNoBareRoleLineIsEmitted() {
        String md = ClaudeTranscriptFormatter.toRolePrefixedMarkdown(List.of(
                new ClaudeTurn("user", "keep me"),
                new ClaudeTurn("assistant", "   "),
                new ClaudeTurn("user", "keep me too")));

        assertEquals("USER: keep me\n\nUSER: keep me too", md);
        assertTrue(!md.contains("ASSISTANT:"), "blank assistant turn should be dropped");
    }

    @Test
    void emptyTranscriptProducesEmptyString() {
        assertEquals("", ClaudeTranscriptFormatter.toRolePrefixedMarkdown(List.of()));
    }

    @Test
    void unknownRoleIsUppercasedNotDropped() {
        String md = ClaudeTranscriptFormatter.toRolePrefixedMarkdown(List.of(
                new ClaudeTurn("tool", "ran a search")));

        assertEquals("TOOL: ran a search", md);
    }
}
