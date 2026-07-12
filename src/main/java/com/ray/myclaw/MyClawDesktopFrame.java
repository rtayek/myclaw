package com.ray.myclaw;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
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

    private final PromptService promptService;
    private final JComboBox<BackendChoice> backendCombo;
    private final JTextArea transcriptArea;
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
        this.transcriptArea = new JTextArea();
        this.promptArea = new JTextArea(6, 60);
        this.sendButton = new JButton("Send");
        this.clearButton = new JButton("Clear");
        this.statusLabel = new JLabel("Ready");
        this.progressBar = new JProgressBar();

        configureFrame();
        configureTextAreas();
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

    private void configureTextAreas() {
        Font readableFont = new Font(Font.MONOSPACED, Font.PLAIN, 18);
        transcriptArea.setEditable(false);
        transcriptArea.setLineWrap(true);
        transcriptArea.setWrapStyleWord(true);
        transcriptArea.setFont(readableFont);
        transcriptArea.setMargin(new Insets(12, 12, 12, 12));

        promptArea.setEditable(true);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setFont(readableFont);
        promptArea.setMargin(new Insets(12, 12, 12, 12));
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
        topPanel.add(clearButton, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(transcriptArea),
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
        clearButton.addActionListener(event -> {
            transcriptArea.setText("");
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
            appendBlock("Error", "Prompt is empty.");
            showReady();
            promptArea.requestFocusInWindow();
            return;
        }

        BackendChoice backend = (BackendChoice) backendCombo.getSelectedItem();
        if (backend == null) {
            appendBlock("Error", "No backend selected.");
            return;
        }

        appendBlock("You", prompt);
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
                    appendBlock(result.backendLabel(), result.response());
                    showSucceeded();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    appendBlock(backend.label() + " error", "Interrupted while waiting for response.");
                    showFailed();
                } catch (ExecutionException exception) {
                    appendBlock(backend.label() + " error", DesktopErrorFormatter.messageFor(exception.getCause()));
                    showFailed();
                } catch (RuntimeException exception) {
                    appendBlock(backend.label() + " error", DesktopErrorFormatter.messageFor(exception));
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

    private void appendBlock(String label, String text) {
        if (!transcriptArea.getText().isEmpty()) {
            transcriptArea.append("\n\n");
        }
        transcriptArea.append(label);
        transcriptArea.append(":\n");
        transcriptArea.append(text);
        if (!text.endsWith("\n")) {
            transcriptArea.append("\n");
        }
        transcriptArea.setCaretPosition(transcriptArea.getDocument().getLength());
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
        return transcriptArea.getText();
    }

    void setPromptText(String prompt) {
        promptArea.setText(prompt);
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
