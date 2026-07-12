package myclaw.execution;

public interface CommandExecutor {
    CommandResult run(CommandRequest request);
}
