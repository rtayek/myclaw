package myclaw.cli;

import java.time.Clock;

import myclaw.application.ApplicationBackends;
import myclaw.application.HarnessMainApplication;
import myclaw.application.PromptService;
import myclaw.transcript.ResultReporter;
import myclaw.transcript.TranscriptWriter;

import java.nio.file.Path;

public final class HarnessMain {
    private HarnessMain() {
    }

    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        int exitCode = new HarnessMainApplication(
                new PromptService(ApplicationBackends.create(), new TranscriptWriter(Path.of("runs"), clock), clock),
                new ResultReporter(System.out, System.err),
                System.in
        ).run(args);
        System.exit(exitCode);
    }
}
