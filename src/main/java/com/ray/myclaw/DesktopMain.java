package com.ray.myclaw;

import java.nio.file.Path;
import java.time.Clock;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DesktopMain {
    private DesktopMain() {
    }

    public static void main(String[] args) {
        installSystemLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            Clock clock = Clock.systemUTC();
            PromptService promptService = new PromptService(
                    ApplicationBackends.create(),
                    new TranscriptWriter(Path.of("runs"), clock),
                    clock
            );
            MyClawDesktopFrame frame = new MyClawDesktopFrame(promptService);
            frame.setVisible(true);
        });
    }

    private static void installSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            System.err.println("Could not install system look and feel: " + exception.getMessage());
        }
    }
}
