package myclaw.execution;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommandRunnerTest {
    private final CommandRunner runner = new CommandRunner();

    @Test
    void capturesStdout() {
        CommandResult result = runHelper("stdout", "hello");

        assertEquals(0, result.exitCode());
        assertEquals("hello", result.standardOutput());
        assertEquals("", result.standardError());
    }

    @Test
    void capturesStderr() {
        CommandResult result = runHelper("stderr", "careful");

        assertEquals(0, result.exitCode());
        assertEquals("", result.standardOutput());
        assertEquals("careful", result.standardError());
    }

    @Test
    void preservesNonzeroExitStatus() {
        CommandResult result = runHelper("exit", "17");

        assertEquals(17, result.exitCode());
        assertFalse(result.timedOut());
    }

    @Test
    void passesArgumentsWithoutShellInterpretation() {
        String argument = "one; echo BAD && $HOME `date` \"quoted\"\nnext";

        CommandResult result = runHelper("args", argument);

        assertEquals(argument, result.standardOutput());
    }

    @Test
    void writesStandardInput() {
        CommandResult result = runner.run(new CommandRequest(helperCommand("stdin"), "input\nline", Duration.ofSeconds(5)));

        assertEquals("input\nline", result.standardOutput());
    }

    @Test
    void recordsDuration() {
        CommandResult result = runHelper("stdout", "done");

        assertFalse(result.duration().isNegative());
        assertFalse(result.duration().isZero());
    }

    @Test
    void timesOut() {
        CommandResult result = runner.run(new CommandRequest(helperCommand("sleep", "5000"), "", Duration.ofMillis(200)));

        assertTrue(result.timedOut());
        assertEquals(-1, result.exitCode());
    }

    @Test
    void reportsMissingExecutableClearly() {
        CommandExecutionException exception = assertThrows(
                CommandExecutionException.class,
                () -> runner.run(new CommandRequest(List.of("definitely-not-a-command-550e8400"), "", Duration.ofSeconds(1)))
        );

        assertTrue(exception.getMessage().contains("Could not start command"));
    }

    @Test
    void handlesSimultaneousLargeStdoutAndStderrWithoutDeadlock() {
        CommandResult result = runHelper("largeBoth", "65536");

        assertEquals(0, result.exitCode());
        assertEquals(65_536, result.standardOutput().length());
        assertEquals(65_536, result.standardError().length());
    }

    @Test
    void preservesUnicode() {
        String text = "snowman \u2603 kanji \u6f22 emoji \uD83D\uDE80";

        CommandResult result = runHelper("unicode");

        assertEquals(text, result.standardOutput());
    }

    @Test
    void preservesEmbeddedQuotesAndShellMetacharacters() {
        String text = "\"single' ; && || $PATH `whoami` < > |";

        CommandResult result = runHelper("stdout", text);

        assertEquals(text, result.standardOutput());
    }

    private CommandResult runHelper(String... args) {
        return runner.run(new CommandRequest(helperCommand(args), "", Duration.ofSeconds(5)));
    }

    private static List<String> helperCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.add("-Dfile.encoding=UTF-8");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(CommandTestProgram.class.getName());
        command.addAll(List.of(args));
        return command;
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }
}
