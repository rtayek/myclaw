package myclaw.desktop;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import myclaw.application.PromptResult;
import myclaw.application.PromptService;

final class MyClawDesktopFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final List<BackendChoice> BACKEND_CHOICES = List.of(
            new BackendChoice("claude", "Claude"),
            new BackendChoice("glm", "GLM")
    );
    private static final String DEFAULT_FONT_FAMILY = Font.MONOSPACED;
    private static final int DEFAULT_FONT_SIZE = 18;
    private static final List<Integer> FONT_SIZES = List.of(14, 16, 18, 20, 24, 28, 32, 36, 42, 48);
    private static final String SEND_READY_ACCESSIBLE_NAME = "Send";
    private static final String SEND_READY_ACCESSIBLE_DESCRIPTION = "Send the prompt to the selected AI backend.";

    private final PromptService promptService;
    private final JComboBox<BackendChoice> backendCombo;
    private final JComboBox<String> fontFamilyCombo;
    private final JComboBox<Integer> fontSizeCombo;
    private final TranscriptView transcriptView;
    private final JScrollPane transcriptScroll;
    private final JTextArea promptArea;
    private final JButton sendButton;
    private final JButton clearButton;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final ClipboardWriter clipboardWriter;
    private Action sendAction;
    private Action clearTranscriptAction;
    private Action detachTranscriptAction;
    private Action copyTranscriptAction;
    private Action focusPromptAction;
    private Action keyboardShortcutsAction;
    private Action aboutAction;
    private Action zoomInAction;
    private Action zoomOutAction;
    private Action zoomResetAction;
    private JSplitPane splitPane;
    private JDialog transcriptDialog;
    private JButton reattachButton;
    private JLabel backendLabel;
    private JLabel fontLabel;
    private JLabel sizeLabel;
    private int savedDividerLocation;
    private boolean requestActive;

    MyClawDesktopFrame(PromptService promptService) {
        this(promptService, new ThemeManager());
    }

    MyClawDesktopFrame(PromptService promptService, ThemeManager themeManager) {
        this(promptService, themeManager, text ->
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null));
    }

    MyClawDesktopFrame(PromptService promptService, ThemeManager themeManager, ClipboardWriter clipboardWriter) {
        super("myclaw");
        this.promptService = Objects.requireNonNull(promptService, "promptService");
        Objects.requireNonNull(themeManager, "themeManager");
        this.clipboardWriter = Objects.requireNonNull(clipboardWriter, "clipboardWriter");
        this.backendCombo = new JComboBox<>(BACKEND_CHOICES.toArray(BackendChoice[]::new));
        this.fontFamilyCombo = new JComboBox<>(availableFontFamilies().toArray(String[]::new));
        this.fontSizeCombo = new JComboBox<>(FONT_SIZES.toArray(Integer[]::new));
        this.transcriptView = new TranscriptView();
        this.transcriptScroll = new JScrollPane(transcriptView.component());
        this.promptArea = new JTextArea(6, 60);
        this.sendButton = new JButton("Send");
        this.clearButton = new JButton("Clear");
        this.statusLabel = new JLabel("Ready");
        this.progressBar = new JProgressBar();

        createActions();
        configureFrame();
        configurePromptArea();
        configureFontControls();
        configureProgressBar();
        configureTooltipsAndAccessibility();
        setJMenuBar(menuBar(themeManager));
        setContentPane(contentPanel());
        pack();
        setLocationRelativeTo(null);
        wireActions();
        configureZoom();
        bindGlobalKeys();
        showReady();
    }

    @Override
    public void dispose() {
        if (transcriptDialog != null) {
            transcriptDialog.dispose();
        }
        super.dispose();
    }

    private void createActions() {
        sendAction = new AbstractAction("Send") {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendPrompt();
            }
        };
        sendAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
        sendAction.putValue(Action.SHORT_DESCRIPTION, "Send prompt (Ctrl+Enter)");

        clearTranscriptAction = new AbstractAction("Clear Transcript") {
            @Override
            public void actionPerformed(ActionEvent event) {
                transcriptView.clear();
                promptArea.requestFocusInWindow();
                statusLabel.setText("Transcript cleared");
            }
        };
        clearTranscriptAction.putValue(Action.SHORT_DESCRIPTION, "Clear the visible transcript");

        detachTranscriptAction = new AbstractAction("Detach Transcript") {
            @Override
            public void actionPerformed(ActionEvent event) {
                toggleTranscriptDetached();
            }
        };
        detachTranscriptAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
        detachTranscriptAction.putValue(Action.SHORT_DESCRIPTION, "Detach or reattach the transcript");

        copyTranscriptAction = new AbstractAction("Copy Transcript") {
            @Override
            public void actionPerformed(ActionEvent event) {
                copyTranscript();
            }
        };
        copyTranscriptAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        copyTranscriptAction.putValue(Action.SHORT_DESCRIPTION, "Copy the complete transcript");

        focusPromptAction = new AbstractAction("Focus Prompt") {
            @Override
            public void actionPerformed(ActionEvent event) {
                promptArea.requestFocusInWindow();
            }
        };
        focusPromptAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        focusPromptAction.putValue(Action.SHORT_DESCRIPTION, "Focus the prompt editor");

        keyboardShortcutsAction = new AbstractAction("Keyboard Shortcuts") {
            @Override
            public void actionPerformed(ActionEvent event) {
                showKeyboardShortcuts();
            }
        };
        keyboardShortcutsAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        keyboardShortcutsAction.putValue(Action.SHORT_DESCRIPTION, "Show keyboard shortcuts");

        aboutAction = new AbstractAction("About myclaw") {
            @Override
            public void actionPerformed(ActionEvent event) {
                showAbout();
            }
        };
        aboutAction.putValue(Action.SHORT_DESCRIPTION, "About myclaw");

        zoomInAction = new AbstractAction("Zoom In") {
            @Override
            public void actionPerformed(ActionEvent event) {
                zoomIn();
            }
        };
        zoomInAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));

        zoomOutAction = new AbstractAction("Zoom Out") {
            @Override
            public void actionPerformed(ActionEvent event) {
                zoomOut();
            }
        };
        zoomOutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));

        zoomResetAction = new AbstractAction("Reset Text Size") {
            @Override
            public void actionPerformed(ActionEvent event) {
                zoomReset();
            }
        };
        zoomResetAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
    }

    private JMenuBar menuBar(ThemeManager themeManager) {
        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup group = new ButtonGroup();
        ThemeOption current = themeManager.currentTheme();
        for (ThemeOption option : ThemeOption.ALL) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(option.label(), option.equals(current));
            item.addActionListener(event -> themeManager.apply(option));
            group.add(item);
            themeMenu.add(item);
        }

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(copyTranscriptAction));
        editMenu.add(new JMenuItem(clearTranscriptAction));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem(focusPromptAction));

        JMenu zoomMenu = new JMenu("Zoom");
        zoomMenu.add(new JMenuItem(zoomInAction));
        zoomMenu.add(new JMenuItem(zoomOutAction));
        zoomMenu.add(new JMenuItem(zoomResetAction));

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(themeMenu);
        viewMenu.add(zoomMenu);
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem(detachTranscriptAction));

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem(keyboardShortcutsAction));
        helpMenu.add(new JMenuItem(aboutAction));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setPreferredSize(new Dimension(1050, 780));
    }

    private void configurePromptArea() {
        promptArea.setEditable(true);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setMargin(new Insets(12, 12, 12, 12));
    }

    private void configureFontControls() {
        fontFamilyCombo.setSelectedItem(DEFAULT_FONT_FAMILY);
        fontSizeCombo.setSelectedItem(DEFAULT_FONT_SIZE);
        applySelectedTextFont();
    }

    private void configureProgressBar() {
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(180, 22));
        sendButton.setMargin(new Insets(8, 18, 8, 18));
    }

    private void configureTooltipsAndAccessibility() {
        // sendButton and clearButton get their tooltip from the shared Action's
        // SHORT_DESCRIPTION (set in createActions()) once wireActions() calls setAction().
        backendCombo.setToolTipText("Choose the AI backend");
        fontFamilyCombo.setToolTipText("Choose the text font");
        fontSizeCombo.setToolTipText("Change transcript and prompt text size");
        progressBar.setToolTipText("Shows when a request is running");

        backendCombo.getAccessibleContext().setAccessibleName("Backend selector");
        backendCombo.getAccessibleContext().setAccessibleDescription("Choose the AI backend for the next prompt.");
        fontFamilyCombo.getAccessibleContext().setAccessibleName("Font selector");
        fontFamilyCombo.getAccessibleContext().setAccessibleDescription("Choose the font used by the transcript and prompt editor.");
        fontSizeCombo.getAccessibleContext().setAccessibleName("Text size selector");
        fontSizeCombo.getAccessibleContext().setAccessibleDescription("Change the text size used by the transcript and prompt editor.");
        transcriptView.component().getAccessibleContext().setAccessibleName("Transcript");
        transcriptView.component().getAccessibleContext().setAccessibleDescription("Conversation transcript containing user prompts, AI replies, and errors.");
        promptArea.getAccessibleContext().setAccessibleName("Prompt editor");
        promptArea.getAccessibleContext().setAccessibleDescription("Enter or dictate a prompt for the selected AI backend.");
        sendButton.getAccessibleContext().setAccessibleName(SEND_READY_ACCESSIBLE_NAME);
        sendButton.getAccessibleContext().setAccessibleDescription(SEND_READY_ACCESSIBLE_DESCRIPTION);
        clearButton.getAccessibleContext().setAccessibleName("Clear transcript");
        clearButton.getAccessibleContext().setAccessibleDescription("Clear the visible transcript.");
        statusLabel.getAccessibleContext().setAccessibleName("Status");
        statusLabel.getAccessibleContext().setAccessibleDescription("Current request status.");
        progressBar.getAccessibleContext().setAccessibleName("Request progress");
        progressBar.getAccessibleContext().setAccessibleDescription("Indicates that a request is running.");
    }

    private JPanel contentPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel topPanel = new JPanel(new BorderLayout(12, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        backendLabel = new JLabel("Backend:");
        backendLabel.setLabelFor(backendCombo);
        topPanel.add(backendLabel, BorderLayout.WEST);
        topPanel.add(backendCombo, BorderLayout.CENTER);

        JPanel viewControls = new JPanel(new BorderLayout(8, 8));
        fontLabel = new JLabel("Font:");
        fontLabel.setLabelFor(fontFamilyCombo);
        viewControls.add(fontLabel, BorderLayout.WEST);
        viewControls.add(fontFamilyCombo, BorderLayout.CENTER);

        JPanel sizeControls = new JPanel(new BorderLayout(8, 8));
        sizeLabel = new JLabel("Size:");
        sizeLabel.setLabelFor(fontSizeCombo);
        sizeControls.add(sizeLabel, BorderLayout.WEST);
        sizeControls.add(fontSizeCombo, BorderLayout.CENTER);
        viewControls.add(sizeControls, BorderLayout.EAST);

        JPanel rightControls = new JPanel(new BorderLayout(8, 8));
        rightControls.add(viewControls, BorderLayout.CENTER);
        rightControls.add(clearButton, BorderLayout.EAST);
        topPanel.add(rightControls, BorderLayout.EAST);

        transcriptScroll.setBorder(BorderFactory.createTitledBorder("Transcript"));

        JScrollPane promptScroll = new JScrollPane(promptArea);
        promptScroll.setBorder(BorderFactory.createTitledBorder("Prompt"));
        promptScroll.setMinimumSize(new Dimension(200, 150));
        promptScroll.setPreferredSize(new Dimension(900, 210));

        splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                transcriptScroll,
                promptScroll
        );
        splitPane.setResizeWeight(0.75);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(10);

        JPanel bottomPanel = new JPanel(new BorderLayout(12, 8));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout(8, 8));
        actionPanel.add(progressBar, BorderLayout.CENTER);
        actionPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(actionPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void wireActions() {
        sendButton.setAction(sendAction);
        fontFamilyCombo.addActionListener(event -> applySelectedTextFont());
        fontSizeCombo.addActionListener(event -> applySelectedTextFont());
        clearButton.setAction(clearTranscriptAction);
        clearButton.setText("Clear");
        promptArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateSendEnabled();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateSendEnabled();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateSendEnabled();
            }
        });

        promptArea.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), "sendPrompt");
        promptArea.getActionMap().put("sendPrompt", sendAction);

        promptArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "newlineOrSendOnDoubleEnter");
        promptArea.getActionMap().put("newlineOrSendOnDoubleEnter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleEnterKey();
            }
        });
    }

    private void handleEnterKey() {
        String text = promptArea.getText();
        boolean caretAtEnd = promptArea.getCaretPosition() == text.length();
        if (caretAtEnd && text.endsWith("\n") && !text.isBlank()) {
            promptArea.setText(text.stripTrailing());
            sendPrompt();
        } else {
            promptArea.replaceSelection("\n");
        }
    }

    private void configureZoom() {
        installZoomWheelListener(transcriptView.component());
        installZoomWheelListener(promptArea);

        // promptArea never leaves the main frame, so the root-pane WHEN_IN_FOCUSED_WINDOW
        // bindings in bindGlobalKeys() already cover keyboard zoom while it has focus.
        // transcriptView's component needs its own binding because it can be reparented
        // into the detached transcript dialog, a different focused window.
        bindZoomKeys(transcriptView.component());
    }

    private void installZoomWheelListener(JComponent component) {
        component.addMouseWheelListener(event -> {
            if ((event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                event.consume();
                if (event.getWheelRotation() < 0) {
                    zoomIn();
                } else if (event.getWheelRotation() > 0) {
                    zoomOut();
                }
                return;
            }

            // Registering a MouseWheelListener directly on a component stops Swing from
            // auto-forwarding unconsumed wheel events to the ancestor JScrollPane, so plain
            // (non-zoom) scrolling has to be forwarded explicitly.
            JScrollPane ancestorScrollPane =
                    (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, component);
            if (ancestorScrollPane != null) {
                ancestorScrollPane.dispatchEvent(event);
            }
        });
    }

    private void bindZoomKeys(JComponent component) {
        var inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke("control PLUS"), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke("control ADD"), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke("control shift EQUALS"), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke("control 0"), "zoomReset");

        actionMap.put("zoomIn", zoomInAction);
        actionMap.put("zoomOut", zoomOutAction);
        actionMap.put("zoomReset", zoomResetAction);
    }

    private void bindGlobalKeys() {
        var inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "sendPrompt");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "detachTranscript");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "focusPrompt");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "copyTranscript");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "keyboardShortcuts");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "zoomReset");

        actionMap.put("sendPrompt", sendAction);
        actionMap.put("detachTranscript", detachTranscriptAction);
        actionMap.put("focusPrompt", focusPromptAction);
        actionMap.put("copyTranscript", copyTranscriptAction);
        actionMap.put("keyboardShortcuts", keyboardShortcutsAction);
        actionMap.put("zoomIn", zoomInAction);
        actionMap.put("zoomOut", zoomOutAction);
        actionMap.put("zoomReset", zoomResetAction);
    }

    void zoomIn() {
        stepFontSize(1);
    }

    void zoomOut() {
        stepFontSize(-1);
    }

    void zoomReset() {
        fontSizeCombo.setSelectedItem(DEFAULT_FONT_SIZE);
    }

    private void stepFontSize(int direction) {
        int currentIndex = FONT_SIZES.indexOf(fontSizeCombo.getSelectedItem());
        if (currentIndex < 0) {
            return;
        }
        int nextIndex = Math.max(0, Math.min(FONT_SIZES.size() - 1, currentIndex + direction));
        if (nextIndex != currentIndex) {
            fontSizeCombo.setSelectedItem(FONT_SIZES.get(nextIndex));
        }
    }

    void detachTranscript() {
        if (transcriptDialog != null) {
            return;
        }

        savedDividerLocation = splitPane.getDividerLocation();
        JPanel placeholder = new JPanel(new BorderLayout(8, 8));
        placeholder.setBorder(BorderFactory.createTitledBorder("Transcript"));
        JLabel placeholderLabel = new JLabel("Transcript detached.", SwingConstants.CENTER);
        reattachButton = new JButton(detachTranscriptAction);
        reattachButton.setText("Reattach");
        reattachButton.setToolTipText("Reattach the transcript to the main window");
        reattachButton.getAccessibleContext().setAccessibleName("Reattach transcript");
        reattachButton.getAccessibleContext().setAccessibleDescription("Reattach the transcript to the main window.");
        placeholder.add(placeholderLabel, BorderLayout.CENTER);
        placeholder.add(reattachButton, BorderLayout.SOUTH);
        splitPane.setTopComponent(placeholder);
        splitPane.setDividerLocation(savedDividerLocation);

        transcriptDialog = new JDialog(this, "myclaw - Transcript", false);
        transcriptDialog.getAccessibleContext().setAccessibleName("Detached transcript");
        transcriptDialog.getAccessibleContext().setAccessibleDescription("Detached window containing the conversation transcript.");
        transcriptDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        transcriptDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                reattachTranscript();
            }
        });
        transcriptDialog.setContentPane(transcriptScroll);
        transcriptDialog.setSize(700, 600);
        transcriptDialog.setLocationRelativeTo(this);
        transcriptDialog.setVisible(true);

        detachTranscriptAction.putValue(Action.NAME, "Reattach Transcript");
        statusLabel.setText("Transcript detached");
    }

    void reattachTranscript() {
        if (transcriptDialog == null) {
            return;
        }

        splitPane.setTopComponent(transcriptScroll);
        splitPane.setDividerLocation(savedDividerLocation);

        transcriptDialog.dispose();
        transcriptDialog = null;
        reattachButton = null;

        detachTranscriptAction.putValue(Action.NAME, "Detach Transcript");
        statusLabel.setText("Transcript reattached");
    }

    boolean transcriptDetached() {
        return transcriptDialog != null;
    }

    private void toggleTranscriptDetached() {
        if (transcriptDialog == null) {
            detachTranscript();
        } else {
            reattachTranscript();
        }
    }

    private void copyTranscript() {
        clipboardWriter.copy(transcriptView.text());
        statusLabel.setText(transcriptView.text().isEmpty() ? "Empty transcript copied" : "Transcript copied");
    }

    private JDialog showKeyboardShortcuts() {
        return showTextDialog(
                "Keyboard Shortcuts",
                "Keyboard shortcuts",
                "List of keyboard shortcuts available in myclaw.",
                keyboardShortcutsText());
    }

    private JDialog showAbout() {
        return showTextDialog(
                "About myclaw",
                "About myclaw",
                "Information about myclaw.",
                "myclaw\n\nAn accessible Java desktop workbench for local and command-line AI systems.");
    }

    private JDialog showTextDialog(String title, String accessibleName, String accessibleDescription, String content) {
        JTextArea textArea = new JTextArea(content, 10, 42);
        textArea.setEditable(false);
        textArea.setFont(promptArea.getFont());
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        textArea.setCaretPosition(0);
        textArea.getAccessibleContext().setAccessibleName(accessibleName);
        textArea.getAccessibleContext().setAccessibleDescription(accessibleDescription);

        JDialog dialog = new JDialog(this, title, false);
        dialog.getAccessibleContext().setAccessibleName(accessibleName);
        dialog.getAccessibleContext().setAccessibleDescription(accessibleDescription);
        dialog.setContentPane(new JScrollPane(textArea));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return dialog;
    }

    private static String keyboardShortcutsText() {
        return """
                Ctrl+Enter       Send prompt
                Enter twice      Send prompt
                Ctrl+D           Detach or reattach transcript
                Ctrl+L           Focus prompt
                Ctrl+Shift+C     Copy transcript
                Ctrl++           Zoom in
                Ctrl+-           Zoom out
                Ctrl+0           Reset text size
                F1               Show this help
                """;
    }

    private void sendPrompt() {
        if (requestActive) {
            return;
        }

        String prompt = promptArea.getText();
        if (prompt.isBlank()) {
            transcriptView.appendError("Error", "Prompt is empty.");
            showReady();
            promptArea.requestFocusInWindow();
            return;
        }

        BackendChoice backend = (BackendChoice) backendCombo.getSelectedItem();
        if (backend == null) {
            transcriptView.appendError("Error", "No backend selected.");
            return;
        }

        transcriptView.appendUser(prompt);
        promptArea.setText("");
        showWorking(backend);

        SwingWorker<PromptResult, Void> worker = new SwingWorker<>() {
            @Override
            protected PromptResult doInBackground() {
                return promptService.submit(backend.id(), prompt);
            }

            @Override
            protected void done() {
                try {
                    PromptResult result = get();
                    transcriptView.appendAssistant(result.backendLabel(), result.response());
                    showSucceeded();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    transcriptView.appendError(backend.label() + " error", "Interrupted while waiting for response.");
                    showFailed();
                } catch (ExecutionException exception) {
                    transcriptView.appendError(backend.label() + " error", DesktopErrorFormatter.messageFor(exception.getCause()));
                    showFailed();
                } catch (RuntimeException exception) {
                    transcriptView.appendError(backend.label() + " error", DesktopErrorFormatter.messageFor(exception));
                    showFailed();
                } finally {
                    promptArea.requestFocusInWindow();
                }
            }
        };
        worker.execute();
    }

    void showReady() {
        requestActive = false;
        statusLabel.setText("Ready");
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        setCursor(Cursor.getDefaultCursor());
        backendCombo.setEnabled(true);
        promptArea.setEnabled(true);
        clearTranscriptAction.setEnabled(true);
        sendButton.setText("Send");
        restoreSendAccessibleState();
        updateSendEnabled();
    }

    void showWorking(BackendChoice backend) {
        requestActive = true;
        statusLabel.setText("Waiting for " + backend.label() + "...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        sendAction.setEnabled(false);
        sendButton.setText("Working...");
        sendButton.getAccessibleContext().setAccessibleName("Send, working");
        sendButton.getAccessibleContext().setAccessibleDescription(
                "Waiting for " + backend.label() + ". Send is unavailable until the request finishes.");
        backendCombo.setEnabled(false);
        promptArea.setEnabled(true);
        clearTranscriptAction.setEnabled(false);
    }

    void showSucceeded() {
        showReady();
    }

    void showFailed() {
        requestActive = false;
        statusLabel.setText("Failed");
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        setCursor(Cursor.getDefaultCursor());
        backendCombo.setEnabled(true);
        promptArea.setEnabled(true);
        clearTranscriptAction.setEnabled(true);
        sendButton.setText("Send");
        restoreSendAccessibleState();
        updateSendEnabled();
    }

    private void restoreSendAccessibleState() {
        sendButton.getAccessibleContext().setAccessibleName(SEND_READY_ACCESSIBLE_NAME);
        sendButton.getAccessibleContext().setAccessibleDescription(SEND_READY_ACCESSIBLE_DESCRIPTION);
    }

    private void updateSendEnabled() {
        sendAction.setEnabled(!requestActive && !promptArea.getText().isBlank());
    }

    private void applySelectedTextFont() {
        Object selectedFamily = fontFamilyCombo.getSelectedItem();
        Object selectedSize = fontSizeCombo.getSelectedItem();
        if (!(selectedFamily instanceof String family) || !(selectedSize instanceof Integer size)) {
            return;
        }
        Font selectedFont = new Font(family, Font.PLAIN, size);
        transcriptView.setFont(selectedFont);
        promptArea.setFont(selectedFont);
    }

    private static List<String> availableFontFamilies() {
        TreeSet<String> families = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        families.add(DEFAULT_FONT_FAMILY);
        families.add(Font.SANS_SERIF);
        families.add(Font.SERIF);
        families.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        return new ArrayList<>(families);
    }

    boolean requestActive() {
        return requestActive;
    }

    String statusText() {
        return statusLabel.getText();
    }

    boolean sendEnabled() {
        return sendButton.isEnabled();
    }

    boolean backendSelectionEnabled() {
        return backendCombo.isEnabled();
    }

    boolean progressActive() {
        return progressBar.isVisible() && progressBar.isIndeterminate();
    }

    String transcriptText() {
        return transcriptView.text();
    }

    void setPromptText(String prompt) {
        promptArea.setText(prompt);
    }

    void selectFontFamilyForTest(String family) {
        fontFamilyCombo.setSelectedItem(family);
    }

    void selectFontSizeForTest(int size) {
        fontSizeCombo.setSelectedItem(size);
    }

    Font promptFontForTest() {
        return promptArea.getFont();
    }

    Font transcriptFontForTest() {
        return transcriptView.currentFont();
    }

    void selectBackend(String backendId) {
        for (int index = 0; index < backendCombo.getItemCount(); index++) {
            BackendChoice choice = backendCombo.getItemAt(index);
            if (choice.id().equals(backendId)) {
                backendCombo.setSelectedIndex(index);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown desktop backend: " + backendId);
    }

    void submitForTest() {
        sendPrompt();
    }

    Action actionForTest(String name) {
        return switch (name) {
            case "sendPrompt" -> sendAction;
            case "clearTranscript" -> clearTranscriptAction;
            case "detachTranscript" -> detachTranscriptAction;
            case "copyTranscript" -> copyTranscriptAction;
            case "focusPrompt" -> focusPromptAction;
            case "keyboardShortcuts" -> keyboardShortcutsAction;
            case "about" -> aboutAction;
            case "zoomIn" -> zoomInAction;
            case "zoomOut" -> zoomOutAction;
            case "zoomReset" -> zoomResetAction;
            default -> throw new IllegalArgumentException("Unknown desktop action: " + name);
        };
    }

    Object keyBindingForTest(KeyStroke keyStroke) {
        return getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(keyStroke);
    }

    Action menuActionForTest(String menuName, String itemName) {
        JMenuBar menuBar = getJMenuBar();
        for (int menuIndex = 0; menuIndex < menuBar.getMenuCount(); menuIndex++) {
            JMenu menu = menuBar.getMenu(menuIndex);
            if (menu != null && menuName.equals(menu.getText())) {
                Action found = findMenuItemAction(menu, itemName);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new IllegalArgumentException("Menu item not found: " + menuName + " > " + itemName);
    }

    private static Action findMenuItemAction(JMenu menu, String itemName) {
        for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex++) {
            JMenuItem item = menu.getItem(itemIndex);
            if (item == null) {
                continue;
            }
            if (itemName.equals(item.getText())) {
                return item.getAction();
            }
            if (item instanceof JMenu submenu) {
                Action found = findMenuItemAction(submenu, itemName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    String accessibleNameForTest(String componentName) {
        return switch (componentName) {
            case "backend" -> backendCombo.getAccessibleContext().getAccessibleName();
            case "font" -> fontFamilyCombo.getAccessibleContext().getAccessibleName();
            case "size" -> fontSizeCombo.getAccessibleContext().getAccessibleName();
            case "transcript" -> transcriptView.component().getAccessibleContext().getAccessibleName();
            case "prompt" -> promptArea.getAccessibleContext().getAccessibleName();
            case "send" -> sendButton.getAccessibleContext().getAccessibleName();
            case "clear" -> clearButton.getAccessibleContext().getAccessibleName();
            case "status" -> statusLabel.getAccessibleContext().getAccessibleName();
            case "progress" -> progressBar.getAccessibleContext().getAccessibleName();
            case "reattach" -> reattachButton.getAccessibleContext().getAccessibleName();
            default -> throw new IllegalArgumentException("Unknown component: " + componentName);
        };
    }

    String helpTextForTest() {
        return keyboardShortcutsText();
    }

    String accessibleDescriptionForTest(String componentName) {
        return switch (componentName) {
            case "backend" -> backendCombo.getAccessibleContext().getAccessibleDescription();
            case "font" -> fontFamilyCombo.getAccessibleContext().getAccessibleDescription();
            case "size" -> fontSizeCombo.getAccessibleContext().getAccessibleDescription();
            case "transcript" -> transcriptView.component().getAccessibleContext().getAccessibleDescription();
            case "prompt" -> promptArea.getAccessibleContext().getAccessibleDescription();
            case "send" -> sendButton.getAccessibleContext().getAccessibleDescription();
            case "clear" -> clearButton.getAccessibleContext().getAccessibleDescription();
            case "status" -> statusLabel.getAccessibleContext().getAccessibleDescription();
            case "progress" -> progressBar.getAccessibleContext().getAccessibleDescription();
            default -> throw new IllegalArgumentException("Unknown component: " + componentName);
        };
    }

    boolean labelAssociatedForTest(String componentName) {
        return switch (componentName) {
            case "backend" -> backendLabel.getLabelFor() == backendCombo;
            case "font" -> fontLabel.getLabelFor() == fontFamilyCombo;
            case "size" -> sizeLabel.getLabelFor() == fontSizeCombo;
            default -> throw new IllegalArgumentException("Unknown label: " + componentName);
        };
    }

    boolean promptFocusOwnerForTest() {
        return promptArea.isFocusOwner();
    }

    int promptScrollValueForTest() {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, promptArea);
        return scrollPane.getVerticalScrollBar().getValue();
    }

    void dispatchPromptWheelEventForTest(boolean ctrlDown, int wheelRotation) {
        int modifiers = ctrlDown ? InputEvent.CTRL_DOWN_MASK : 0;
        MouseWheelEvent event = new MouseWheelEvent(
                promptArea, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), modifiers,
                10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, wheelRotation);
        promptArea.dispatchEvent(event);
    }

    Font aboutFontForTest() {
        JDialog dialog = showAbout();
        try {
            JScrollPane scrollPane = (JScrollPane) dialog.getContentPane();
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
            return textArea.getFont();
        } finally {
            dialog.dispose();
        }
    }

    interface ClipboardWriter {
        void copy(String text);
    }
}
