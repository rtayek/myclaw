package com.ray.myclaw;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;

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

    MyClawDesktopFrame(PromptService promptService) {
        super("myclaw");
        this.promptService = Objects.requireNonNull(promptService, "promptService");
        this.backendCombo = new JComboBox<>(BACKEND_CHOICES.toArray(BackendChoice[]::new));
        this.transcriptArea = new JTextArea();
        this.promptArea = new JTextArea(6, 60);
        this.sendButton = new JButton("Send");
        this.clearButton = new JButton("Clear");
        this.statusLabel = new JLabel("Ready");

        configureFrame();
        configureTextAreas();
        setContentPane(contentPanel());
        pack();
        setLocationRelativeTo(null);
        wireActions();
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
        bottomPanel.add(sendButton, BorderLayout.EAST);

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

        promptArea.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), "sendPrompt");
        promptArea.getActionMap().put("sendPrompt", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendPrompt();
            }
        });
    }

    private void sendPrompt() {
        String prompt = promptArea.getText();
        if (prompt.isBlank()) {
            appendBlock("Error", "Prompt is empty.");
            statusLabel.setText("Ready");
            promptArea.requestFocusInWindow();
            return;
        }

        BackendChoice backend = (BackendChoice) backendCombo.getSelectedItem();
        if (backend == null) {
            appendBlock("Error", "No backend selected.");
            return;
        }

        setBusy(true, "Waiting for " + backend.label() + "...");
        appendBlock("You", prompt);
        promptArea.setText("");

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
                    statusLabel.setText("Ready");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    appendBlock(backend.label() + " error", "Interrupted while waiting for response.");
                    statusLabel.setText("Interrupted");
                } catch (ExecutionException exception) {
                    appendBlock(backend.label() + " error", DesktopErrorFormatter.messageFor(exception.getCause()));
                    statusLabel.setText("Failed");
                } finally {
                    setBusy(false, statusLabel.getText());
                    promptArea.requestFocusInWindow();
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy, String status) {
        sendButton.setEnabled(!busy);
        backendCombo.setEnabled(!busy);
        clearButton.setEnabled(!busy);
        statusLabel.setText(status);
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
}
