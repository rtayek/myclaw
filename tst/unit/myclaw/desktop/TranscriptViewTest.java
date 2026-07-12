package myclaw.desktop;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TranscriptViewTest {
    @Test
    void userMessageParagraphsAreRightAligned() throws Exception {
        onEdt(() -> {
            TranscriptView view = new TranscriptView();
            view.appendUser("hello");

            AttributeSet label = paragraphAt(view, view.text().indexOf("You:"));
            AttributeSet body = paragraphAt(view, view.text().indexOf("hello"));

            assertEquals(StyleConstants.ALIGN_RIGHT, StyleConstants.getAlignment(label));
            assertEquals(StyleConstants.ALIGN_RIGHT, StyleConstants.getAlignment(body));
            assertTrue(StyleConstants.getLeftIndent(body) > StyleConstants.getRightIndent(body));
        });
    }

    @Test
    void assistantAndErrorMessageParagraphsAreLeftAligned() throws Exception {
        onEdt(() -> {
            TranscriptView view = new TranscriptView();
            view.appendAssistant("Claude", "response");
            view.appendError("Claude error", "failed");

            assertEquals(
                    StyleConstants.ALIGN_LEFT,
                    StyleConstants.getAlignment(paragraphAt(view, view.text().indexOf("response")))
            );
            assertEquals(
                    StyleConstants.ALIGN_LEFT,
                    StyleConstants.getAlignment(paragraphAt(view, view.text().indexOf("failed")))
            );
        });
    }

    @Test
    void multilineUserMessageUsesOneCoherentRightAlignedBlock() throws Exception {
        onEdt(() -> {
            TranscriptView view = new TranscriptView();
            view.appendUser("first line\nsecond line");

            AttributeSet first = paragraphAt(view, view.text().indexOf("first line"));
            AttributeSet second = paragraphAt(view, view.text().indexOf("second line"));

            assertEquals(StyleConstants.ALIGN_RIGHT, StyleConstants.getAlignment(first));
            assertEquals(StyleConstants.ALIGN_RIGHT, StyleConstants.getAlignment(second));
            assertEquals(StyleConstants.getLeftIndent(first), StyleConstants.getLeftIndent(second));
            assertEquals(StyleConstants.getRightIndent(first), StyleConstants.getRightIndent(second));
        });
    }

    @Test
    void fontChangeUpdatesExistingTextAndPreservesRoleStyling() throws Exception {
        onEdt(() -> {
            TranscriptView view = new TranscriptView();
            view.appendUser("prompt");
            view.appendAssistant("Claude", "response");
            view.appendError("Claude error", "failed");

            StyledDocument document = view.component().getStyledDocument();
            int userLabelOffset = view.text().indexOf("You:");
            int assistantLabelOffset = view.text().indexOf("Claude:");
            int errorLabelOffset = view.text().indexOf("Claude error:");
            int userBodyOffset = view.text().indexOf("prompt");
            Color userColor = StyleConstants.getForeground(characterAt(document, userLabelOffset));
            Color assistantColor = StyleConstants.getForeground(characterAt(document, assistantLabelOffset));
            Color errorColor = StyleConstants.getForeground(characterAt(document, errorLabelOffset));
            AttributeSet userParagraph = paragraphAt(view, userBodyOffset);

            view.setFont(new Font(Font.SERIF, Font.PLAIN, 32));

            assertEquals(Font.SERIF, StyleConstants.getFontFamily(characterAt(document, userLabelOffset)));
            assertEquals(32, StyleConstants.getFontSize(characterAt(document, userLabelOffset)));
            assertEquals(Font.SERIF, StyleConstants.getFontFamily(characterAt(document, userBodyOffset)));
            assertEquals(32, StyleConstants.getFontSize(characterAt(document, userBodyOffset)));
            assertTrue(StyleConstants.isBold(characterAt(document, userLabelOffset)));
            assertEquals(userColor, StyleConstants.getForeground(characterAt(document, userLabelOffset)));
            assertEquals(assistantColor, StyleConstants.getForeground(characterAt(document, assistantLabelOffset)));
            assertEquals(errorColor, StyleConstants.getForeground(characterAt(document, errorLabelOffset)));
            assertEquals(
                    StyleConstants.getAlignment(userParagraph),
                    StyleConstants.getAlignment(paragraphAt(view, userBodyOffset))
            );
        });
    }

    @Test
    void plainTranscriptTextIsUnchangedByFormatting() throws Exception {
        onEdt(() -> {
            TranscriptView view = new TranscriptView();
            view.appendUser("one");
            view.appendAssistant("Claude", "two");

            assertEquals("You:\none\n\n\nClaude:\ntwo\n", view.text());
        });
    }

    @Test
    void clearRemovesAllTranscriptContent() throws Exception {
        onEdt(() -> {
            TranscriptView view = new TranscriptView();
            view.appendUser("one");

            view.clear();

            assertEquals("", view.text());
            assertEquals(0, view.component().getCaretPosition());
        });
    }

    private static AttributeSet paragraphAt(TranscriptView view, int offset) {
        StyledDocument document = view.component().getStyledDocument();
        Element paragraph = document.getParagraphElement(offset);
        return paragraph.getAttributes();
    }

    private static AttributeSet characterAt(StyledDocument document, int offset) {
        Element character = document.getCharacterElement(offset);
        return character.getAttributes();
    }

    private static void onEdt(ThrowingRunnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    action.run();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException && runtimeException.getCause() instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
