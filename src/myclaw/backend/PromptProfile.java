package myclaw.backend;

import java.util.Locale;
import java.util.Objects;

public enum PromptProfile {
    GENERAL {
        @Override
        String applyTo(String prompt) {
            return prompt;
        }
    },
    GUIDED_TEACHING {
        @Override
        String applyTo(String prompt) {
            return """
                    Use guided teaching behavior:
                    - explain clearly;
                    - adapt to the learner's apparent level;
                    - prefer understanding over merely returning an answer;
                    - ask a useful checking question when appropriate;
                    - distinguish uncertainty from established facts;
                    - avoid condescension.

                    Learner prompt:
                    """ + prompt;
        }
    };

    abstract String applyTo(String prompt);

    public static PromptProfile fromExternalName(String name) {
        Objects.requireNonNull(name, "name");
        return switch (name.toLowerCase(Locale.ROOT).replace('_', '-')) {
            case "general" -> GENERAL;
            case "guided-teaching" -> GUIDED_TEACHING;
            default -> throw new IllegalArgumentException("Unknown prompt profile: " + name);
        };
    }
}
