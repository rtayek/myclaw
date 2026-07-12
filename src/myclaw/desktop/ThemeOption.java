package myclaw.desktop;

import java.util.List;
import java.util.Objects;

import javax.swing.UIManager;

record ThemeOption(String label, String lookAndFeelClassName) {
    static final List<ThemeOption> ALL = List.of(
            new ThemeOption("FlatLaf Light", "com.formdev.flatlaf.FlatLightLaf"),
            new ThemeOption("FlatLaf Dark", "com.formdev.flatlaf.FlatDarkLaf"),
            new ThemeOption("FlatLaf IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf"),
            new ThemeOption("FlatLaf Darcula", "com.formdev.flatlaf.FlatDarculaLaf"),
            new ThemeOption("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel"),
            new ThemeOption("System", UIManager.getSystemLookAndFeelClassName())
    );

    static final ThemeOption DEFAULT = ALL.get(0);

    ThemeOption {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(lookAndFeelClassName, "lookAndFeelClassName");
    }

    static ThemeOption byClassName(String lookAndFeelClassName) {
        return ALL.stream()
                .filter(option -> option.lookAndFeelClassName().equals(lookAndFeelClassName))
                .findFirst()
                .orElse(DEFAULT);
    }
}
