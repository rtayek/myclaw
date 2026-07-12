package myclaw;

import java.nio.file.Path;
import java.time.Clock;

import javax.swing.SwingUtilities;

public final class DesktopMain {
    private DesktopMain() {
    }

    public static void main(String[] args) {
        ThemeManager themeManager = new ThemeManager();
        themeManager.apply(themeManager.currentTheme());
        SwingUtilities.invokeLater(() -> {
            Clock clock = Clock.systemUTC();
            PromptService promptService = new PromptService(
                    ApplicationBackends.create(),
                    new TranscriptWriter(Path.of("runs"), clock),
                    clock
            );
            MyClawDesktopFrame frame = new MyClawDesktopFrame(promptService, themeManager);
            frame.setVisible(true);
        });
    }
}
