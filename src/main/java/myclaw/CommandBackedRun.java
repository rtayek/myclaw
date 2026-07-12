package myclaw;

import java.util.List;
import java.util.Objects;

record CommandBackedRun(
        AiResponse response,
        CommandResult commandResult,
        List<String> command
) {
    CommandBackedRun {
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(commandResult, "commandResult");
        command = List.copyOf(Objects.requireNonNull(command, "command"));
    }
}
