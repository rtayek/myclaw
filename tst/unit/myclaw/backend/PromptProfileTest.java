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

        assertTrue(prompt.contains("If the learner asks for a direct answer"));
        assertTrue(prompt.contains("give it plainly"));
        assertTrue(prompt.contains("immediately without quizzing them first"));
    }

    @Test
    void guidedTeachingInstructionContainsHintOnlyBehavior() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("If the learner asks for a hint only"));
        assertTrue(prompt.contains("identify the key step"));
        assertTrue(prompt.contains("without revealing the final answer"));
    }

    @Test
    void guidedTeachingInstructionContainsNoQuestionBehavior() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("If the learner asks not to be questioned"));
        assertTrue(prompt.contains("ending with a question or exercise"));
    }

    @Test
    void guidedTeachingInstructionRespectsRequestedLevelAndMathematicalStyle() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Match any requested technical level"));
        assertTrue(prompt.contains("mathematical style"));
        assertTrue(prompt.contains("nonmathematical style"));
    }

    @Test
    void guidedTeachingInstructionDiscouragesEndlessSocraticQuestioning() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Do not drag out Socratic questioning"));
        assertTrue(prompt.contains("If the learner remains stuck, explain"));
    }

    @Test
    void guidedTeachingInstructionDoesNotRequireHeadingsExercisesOrFollowUpQuestionsEveryTime() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("A checking question"));
        assertTrue(prompt.contains("optional"));
        assertTrue(prompt.contains("Use headings only when"));
    }

    @Test
    void guidedTeachingInstructionDoesNotRequireCallingItselfTheSystem() {
        String prompt = guidedPrompt();

        assertFalse(prompt.contains("Refer to yourself as \"the system\""));
        assertFalse(prompt.contains("call itself \"the system\""));
    }

    @Test
    void guidedTeachingInstructionProhibitsFalseHumanIdentityAndRelationships() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Do not claim"));
        assertTrue(prompt.contains("human feelings"));
        assertTrue(prompt.contains("embodiment"));
        assertTrue(prompt.contains("personal history"));
        assertTrue(prompt.contains("relationship with the learner"));
    }

    @Test
    void guidedTeachingInstructionKeepsResponsesFocusedAndNatural() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Keep the response focused and natural"));
    }

    @Test
    void guidedTeachingInstructionPreservesFactualLimits() {
        String prompt = guidedPrompt();

        assertTrue(prompt.contains("Do not invent facts"));
        assertTrue(prompt.contains("State uncertainty clearly"));
        assertTrue(prompt.contains("verification for disputed"));
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
        assertFalse(prompt.contains("For responses exceeding 150 words"));
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
