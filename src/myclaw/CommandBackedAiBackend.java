package myclaw;

import java.util.List;

interface CommandBackedAiBackend extends AiBackend {
    CommandBackedRun askWithResult(AiRequest request);

    List<String> commandFor(AiRequest request);
}
