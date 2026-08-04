package myclaw.web;

import java.util.List;
import java.util.Locale;

/**
 * Formats a list of {@link ClaudeTurn}s as a role-prefixed Markdown transcript:
 *
 * <pre>
 * USER: first question
 *
 * ASSISTANT: first answer
 *
 * USER: follow-up
 * </pre>
 *
 * This is exactly the shape ChatMap's {@code RolePrefixedTranscriptParser}
 * expects on the way in (its role regex matches {@code user|assistant|system:}
 * case-insensitively), so the two ends stay compatible. Empty and blank turns
 * are dropped so a stray empty DOM node never produces a bare {@code ROLE:} line.
 */
public final class ClaudeTranscriptFormatter {

    private ClaudeTranscriptFormatter() {
    }

    public static String toRolePrefixedMarkdown(List<ClaudeTurn> turns) {
        StringBuilder sb = new StringBuilder();
        for (ClaudeTurn turn : turns) {
            String text = turn.text() == null ? "" : turn.text().strip();
            if (text.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(normalizeRole(turn.role())).append(": ").append(text);
        }
        return sb.toString();
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "user", "human", "you" -> "USER";
            case "assistant", "claude", "ai" -> "ASSISTANT";
            case "system" -> "SYSTEM";
            case "" -> "UNKNOWN";
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }
}
