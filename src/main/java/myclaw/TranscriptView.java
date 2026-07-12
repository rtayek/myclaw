package myclaw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Renders the conversation as styled, selectable, copyable text. Callers speak in terms of
 * who said what (user / assistant / error) rather than a raw text widget, so a future milestone
 * could swap the rendering (e.g. per-message panels) without touching callers.
 */
final class TranscriptView {
    private static final Color USER_LABEL_COLOR = new Color(0x3D7BF5);
    private static final Color ASSISTANT_LABEL_COLOR = new Color(0x1FA971);
    private static final Color ERROR_LABEL_COLOR = new Color(0xE0524A);

    private final JTextPane textPane;
    private Font currentFont;

    TranscriptView() {
        this.textPane = new JTextPane();
        this.currentFont = textPane.getFont();
        textPane.setEditable(false);
        textPane.setMargin(new Insets(14, 16, 14, 16));
    }

    JTextPane component() {
        return textPane;
    }

    Font currentFont() {
        return currentFont;
    }

    void setFont(Font font) {
        this.currentFont = font;
        textPane.setFont(font);
    }

    void clear() {
        textPane.setText("");
    }

    String text() {
        StyledDocument document = textPane.getStyledDocument();
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Could not read transcript text", exception);
        }
    }

    void appendUser(String text) {
        appendMessage("You", text, USER_LABEL_COLOR);
    }

    void appendAssistant(String label, String text) {
        appendMessage(label, text, ASSISTANT_LABEL_COLOR);
    }

    void appendError(String label, String text) {
        appendMessage(label, text, ERROR_LABEL_COLOR);
    }

    private void appendMessage(String label, String text, Color labelColor) {
        StyledDocument document = textPane.getStyledDocument();
        try {
            if (document.getLength() > 0) {
                document.insertString(document.getLength(), "\n\n", bodyAttributes());
            }
            document.insertString(document.getLength(), label + ":\n", labelAttributes(labelColor));
            String body = text.endsWith("\n") ? text : text + "\n";
            document.insertString(document.getLength(), body, bodyAttributes());
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Could not append transcript message", exception);
        }
        textPane.setCaretPosition(document.getLength());
    }

    private SimpleAttributeSet labelAttributes(Color color) {
        SimpleAttributeSet attributes = bodyAttributes();
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setBold(attributes, true);
        return attributes;
    }

    private SimpleAttributeSet bodyAttributes() {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, currentFont.getFamily());
        StyleConstants.setFontSize(attributes, currentFont.getSize());
        return attributes;
    }
}
