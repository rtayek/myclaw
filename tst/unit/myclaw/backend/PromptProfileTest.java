package myclaw.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PromptProfileTest {
    @Test
    void generalRequestsRemainUnchanged() {
        AiRequest request = AiRequest.of("Explain recursion.");

        assertEquals("Explain recursion.", request.effectivePrompt());
        assertEquals("Explain recursion.", request.prompt());
    }

    @Test
    void guidedTeachingInstructionContainsDirectAnswerOverride() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("explicitly demands a direct answer"));
        assertTrue(prompt.contains("output the answer immediately in your first sentence"));
        assertTrue(prompt.contains("static factual, lexical, or historical lookup"));
    }

    @Test
    void guidedTeachingInstructionContainsHintOnlyBehavior() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("If the learner asks for a hint only"));
        assertTrue(prompt.contains("without revealing the final answer"));
        assertTrue(prompt.contains("provide a guiding hint or one clear tracking question"));
    }

    @Test
    void guidedTeachingInstructionContainsNoQuestionBehavior() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("If the learner asks not to be asked questions"));
        assertTrue(prompt.contains("do not end with a question or exercise"));
    }

    @Test
    void guidedTeachingInstructionRespectsRequestedLevelAndMathematicalStyle() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Respect explicit requests for level"));
        assertTrue(prompt.contains("mathematical style"));
        assertTrue(prompt.contains("non-mathematical style"));
    }

    @Test
    void guidedTeachingInstructionDiscouragesEndlessSocraticQuestioning() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Do not continue endless Socratic questioning"));
        assertTrue(prompt.contains("only when the topic permits useful incremental verification"));
    }

    @Test
    void guidedTeachingInstructionUsesHeadersOnlyForLongerResponses() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("For responses exceeding 150 words or two paragraphs"));
        assertTrue(prompt.contains("use semantic markdown headers"));
        assertTrue(prompt.contains("Do not require headings, exercises, or follow-up questions in every response"));
    }

    @Test
    void guidedTeachingInstructionProhibitsSpatialReferences() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Do not use spatial descriptions"));
        assertTrue(prompt.contains("listed above"));
        assertTrue(prompt.contains("as seen above"));
    }

    @Test
    void guidedTeachingInstructionDoesNotRequireCallingItselfTheSystem() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Do not require the response to call itself \"the system\""));
    }

    @Test
    void guidedTeachingInstructionProhibitsFalseHumanIdentityAndRelationships() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("software utility, not a human teacher"));
        assertTrue(prompt.contains("Do not use personal names"));
        assertTrue(prompt.contains("claim emotions"));
        assertTrue(prompt.contains("claim embodiment"));
        assertTrue(prompt.contains("imply personal relationships"));
    }

    @Test
    void guidedTeachingInstructionStartsWithoutPleasantriesOrPraise() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Start your response immediately with the instructional content"));
        assertTrue(prompt.contains("Avoid condescension, empty praise, filler, generic encouragement"));
        assertTrue(prompt.contains("conversational intros such as \"Sure, I can help with that\""));
    }

    @Test
    void guidedTeachingInstructionPreservesFactualLimitsAndUnavailableDataBoundaries() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Distinguish established facts from uncertainty"));
        assertTrue(prompt.contains("Do not guess current local facts, schedules, or unavailable external data"));
    }

    @Test
    void guidedTeachingInstructionCautionsAgainstUnrelatedCodeDumps() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Use code with caution"));
        assertTrue(prompt.contains("rather than dumping unrelated complete code"));
    }

    @Test
    void guidedTeachingPreservesLearnerOriginalText() {
        AiRequest request = AiRequest.withProfile("Help me understand fractions.", PromptProfile.GUIDED_TEACHING);

        assertEquals("Help me understand fractions.", request.prompt());
        assertTrue(request.effectivePrompt().contains("Learner prompt:\nHelp me understand fractions."));
    }

    @Test
    void claudeAndOllamaReceiveTheSameGuidedTeachingInstruction() {
        String claudePrompt = providerPromptForClaude();
        String ollamaInput = providerInputForOllama();
        String marker = "Learner prompt:\nHelp me understand fractions.";

        assertEquals(claudePrompt.substring(0, claudePrompt.indexOf(marker)), ollamaInput.substring(0, ollamaInput.indexOf(marker)));
    }

    @Test
    void guidedTeachingInstructionDoesNotForceExactFollowupOrSystemWordingFromSpec() {
        String prompt = guidedPrompt();

        assertFalse(prompt.contains("End your response with exactly one clear question or exercise"));
        assertFalse(prompt.contains("Refer to yourself as \"the system\""));
    }

    private static String guidedPrompt() {
        return AiRequest.withProfile("Help me understand fractions.", PromptProfile.GUIDED_TEACHING).effectivePrompt();
    }

    private static String providerPromptForClaude() {
        CapturingExecutor executor = new CapturingExecutor();
        executor.result = new myclaw.execution.CommandResult(0, "ok", "", java.time.Duration.ZERO, false);
        new ClaudeCliBackend(executor, java.time.Duration.ofSeconds(5))
                .ask(AiRequest.withProfile("Help me understand fractions.", PromptProfile.GUIDED_TEACHING));
        return executor.request.command().get(2);
    }

    private static String providerInputForOllama() {
        CapturingExecutor executor = new CapturingExecutor();
        executor.result = new myclaw.execution.CommandResult(0, "ok", "", java.time.Duration.ZERO, false);
        new OllamaCliBackend(executor, java.time.Duration.ofSeconds(5), "glm4:9b")
                .ask(AiRequest.withProfile("Help me understand fractions.", PromptProfile.GUIDED_TEACHING));
        return executor.request.standardInput();
    }

    private static final class CapturingExecutor implements myclaw.execution.CommandExecutor {
        private myclaw.execution.CommandRequest request;
        private myclaw.execution.CommandResult result;

        @Override
        public myclaw.execution.CommandResult run(myclaw.execution.CommandRequest request) {
            this.request = request;
            return result;
        }
    }
}
