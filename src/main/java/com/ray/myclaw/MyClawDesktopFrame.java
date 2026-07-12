package com.ray.myclaw;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class MyClawDesktopFrame extends JFrame {
    private static final List<BackendChoice> BACKEND_CHOICES = List.of(
            new BackendChoice("claude", "Claude"),
            new BackendChoice("glm", "GLM")
    );
    private static final String DEFAULT_FONT_FAMILY = Font.MONOSPACED;
    private static final int DEFAULT_FONT_SIZE = 18;
    private static final List<Integer> FONT_SIZES = List.of(14, 16, 18, 20, 24, 28, 32, 36, 42, 48);

    private final PromptService promptService;
    private final JComboBox<BackendChoice> backendCombo;
    private final JComboBox<String> fontFamilyCombo;
    private final JComboBox<Integer> fontSizeCombo;
    private final TranscriptView transcriptView;
    private final JTextArea promptArea;
    private final JButton sendButton;
    private final JButton clearButton;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private boolean requestActive;

    MyClawDesktopFrame(PromptService promptService) {
        this(promptService, new ThemeManager());
    }

    MyClawDesktopFrame(PromptService promptService, ThemeManager themeManager) {
        super("myclaw");
        this.promptService = Objects.requireNonNull(promptService, "promptService");
        Objects.requireNonNull(themeManager, "themeManager");
        this.backendCombo = new JComboBox<>(BACKEND_CHOICES.toArray(BackendChoice[]::new));
        this.fontFamilyCombo = new JComboBox<>(availableFontFamilies().toArray(String[]::new));
        this.fontSizeCombo = new JComboBox<>(FONT_SIZES.toArray(Integer[]::new));
        this.transcriptView = new TranscriptView();
        this.promptArea = new JTextArea(6, 60);
        this.sendButton = new JButton("Send");
        this.clearButton = new JButton("Clear");
        this.statusLabel = new JLabel("Ready");
        this.progressBar = new JProgressBar();

        configureFrame();
        configurePromptArea();
        configureFontControls();
        configureProgressBar();
        setJMenuBar(menuBar(themeManager));
        setContentPane(contentPanel());
        pack();
        setLocationRelativeTo(null);
        wireActions();
        showReady();
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

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(themeMenu);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(viewMenu);
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
    }

    private JPanel contentPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(new JLabel("Backend:"), BorderLayout.WEST);
        topPanel.add(backendCombo, BorderLayout.CENTER);

        JPanel viewControls = new JPanel(new BorderLayout(8, 8));
        viewControls.add(new JLabel("Font:"), BorderLayout.WEST);
        viewControls.add(fontFamilyCombo, BorderLayout.CENTER);

        JPanel sizeControls = new JPanel(new BorderLayout(8, 8));
        sizeControls.add(new JLabel("Size:"), BorderLayout.WEST);
        sizeControls.add(fontSizeCombo, BorderLayout.CENTER);
        viewControls.add(sizeControls, BorderLayout.EAST);

        JPanel rightControls = new JPanel(new BorderLayout(8, 8));
        rightControls.add(viewControls, BorderLayout.CENTER);
        rightControls.add(clearButton, BorderLayout.EAST);
        topPanel.add(rightControls, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(transcriptView.component()),
                new JScrollPane(promptArea)
        );
        splitPane.setResizeWeight(0.75);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
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
        sendButton.addActionListener(event -> sendPrompt());
        fontFamilyCombo.addActionListener(event -> applySelectedTextFont());
        fontSizeCombo.addActionListener(event -> applySelectedTextFont());
        clearButton.addActionListener(event -> {
            transcriptView.clear();
            promptArea.requestFocusInWindow();
        });
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
        promptArea.getActionMap().put("sendPrompt", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendPrompt();
            }
        });

        promptArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "newlineOrSendOnDoubleEnter");
        promptArea.getActionMap().put("newlineOrSendOnDoubleEnter", new javax.swing.AbstractAction() {
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
        clearButton.setEnabled(true);
        sendButton.setText("Send");
        updateSendEnabled();
    }

    void showWorking(BackendChoice backend) {
        requestActive = true;
        statusLabel.setText("Waiting for " + backend.label() + "...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        sendButton.setEnabled(false);
        sendButton.setText("Working...");
        backendCombo.setEnabled(false);
        promptArea.setEnabled(true);
        clearButton.setEnabled(false);
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
        clearButton.setEnabled(true);
        sendButton.setText("Send");
        updateSendEnabled();
    }

    private void updateSendEnabled() {
        sendButton.setEnabled(!requestActive && !promptArea.getText().isBlank());
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
}
