package myclaw.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class TranscriptView {
    private static final Color USER_LABEL_COLOR = new Color(0x3D7BF5);
    private static final Color ASSISTANT_LABEL_COLOR = new Color(0x1FA971);
    private static final Color ERROR_LABEL_COLOR = new Color(0xE0524A);
    private static final int MESSAGE_SIDE_INDENT = 120;
    private static final int MESSAGE_EDGE_INDENT = 18;
    private static final int MESSAGE_SPACE_BEFORE = 8;
    private static final int MESSAGE_SPACE_AFTER = 10;

    private final JTextPane textPane;
    private Font currentFont;

    public TranscriptView() {
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
        StyledDocument document = textPane.getStyledDocument();
        if (document.getLength() > 0) {
            int selectionStart = textPane.getSelectionStart();
            int selectionEnd = textPane.getSelectionEnd();
            int caretPosition = textPane.getCaretPosition();
            document.setCharacterAttributes(0, document.getLength(), fontAttributes(), false);
            if (selectionStart != selectionEnd) {
                textPane.select(selectionStart, selectionEnd);
            } else {
                textPane.setCaretPosition(Math.min(caretPosition, document.getLength()));
            }
        }
    }

    void clear() {
        textPane.setText("");
        textPane.setCaretPosition(0);
    }

    public String text() {
        StyledDocument document = textPane.getStyledDocument();
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Could not read transcript text", exception);
        }
    }

    public void appendUser(String text) {
        appendMessage(TranscriptRole.USER, "You", text);
    }

    void appendAssistant(String label, String text) {
        appendMessage(TranscriptRole.ASSISTANT, label, text);
    }

    void appendError(String label, String text) {
        appendMessage(TranscriptRole.ERROR, label, text);
    }

    private void appendMessage(TranscriptRole role, String label, String text) {
        StyledDocument document = textPane.getStyledDocument();
        try {
            if (document.getLength() > 0) {
                document.insertString(document.getLength(), "\n\n", bodyAttributes());
            }
            int messageStart = document.getLength();
            document.insertString(document.getLength(), label + ":\n", labelAttributes(role));
            String body = text.endsWith("\n") ? text : text + "\n";
            document.insertString(document.getLength(), body, bodyAttributes());
            document.setParagraphAttributes(messageStart, document.getLength() - messageStart, paragraphAttributes(role), false);
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Could not append transcript message", exception);
        }
        textPane.setCaretPosition(document.getLength());
    }

    private SimpleAttributeSet labelAttributes(TranscriptRole role) {
        SimpleAttributeSet attributes = bodyAttributes();
        StyleConstants.setForeground(attributes, labelColor(role));
        StyleConstants.setBold(attributes, true);
        return attributes;
    }

    private SimpleAttributeSet bodyAttributes() {
        return fontAttributes();
    }

    private SimpleAttributeSet fontAttributes() {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, currentFont.getFamily());
        StyleConstants.setFontSize(attributes, currentFont.getSize());
        return attributes;
    }

    private SimpleAttributeSet paragraphAttributes(TranscriptRole role) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(attributes, MESSAGE_SPACE_BEFORE);
        StyleConstants.setSpaceBelow(attributes, MESSAGE_SPACE_AFTER);
        if (role == TranscriptRole.USER) {
            StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_RIGHT);
            StyleConstants.setLeftIndent(attributes, MESSAGE_SIDE_INDENT);
            StyleConstants.setRightIndent(attributes, MESSAGE_EDGE_INDENT);
        } else {
            StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_LEFT);
            StyleConstants.setLeftIndent(attributes, MESSAGE_EDGE_INDENT);
            StyleConstants.setRightIndent(attributes, MESSAGE_SIDE_INDENT);
        }
        return attributes;
    }

    private static Color labelColor(TranscriptRole role) {
        return switch (role) {
            case USER -> USER_LABEL_COLOR;
            case ASSISTANT -> ASSISTANT_LABEL_COLOR;
            case ERROR -> ERROR_LABEL_COLOR;
        };
    }

    private enum TranscriptRole {
        USER,
        ASSISTANT,
        ERROR
    }
}
