package myclaw.desktop;

import java.awt.Window;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

final class ThemeManager {
    private static final String PREFERENCE_KEY = "lookAndFeelClassName";

    private final Preferences preferences;

    ThemeManager() {
        this(Preferences.userNodeForPackage(ThemeManager.class));
    }

    ThemeManager(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    ThemeOption currentTheme() {
        return ThemeOption.byClassName(preferences.get(PREFERENCE_KEY, ThemeOption.DEFAULT.lookAndFeelClassName()));
    }

    void apply(ThemeOption theme) {
        Objects.requireNonNull(theme, "theme");
        try {
            UIManager.setLookAndFeel(theme.lookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException exception) {
            System.err.println("Could not apply theme " + theme.label() + ": " + exception.getMessage());
            return;
        }
        preferences.put(PREFERENCE_KEY, theme.lookAndFeelClassName());
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.pack();
        }
    }
}
