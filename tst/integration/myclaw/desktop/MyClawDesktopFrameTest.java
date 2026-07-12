package myclaw.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import myclaw.application.PromptService;
import myclaw.backend.*;
import myclaw.execution.CommandResult;
import myclaw.transcript.TranscriptWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MyClawDesktopFrameTest {
    @TempDir
    Path tempDir;

    @Test
    void textAreasStartWithReadableDefaultFont() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertEquals(Font.MONOSPACED, frame.promptFontForTest().getFamily());
                assertEquals(18, frame.promptFontForTest().getSize());
                assertEquals(frame.promptFontForTest(), frame.transcriptFontForTest());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void fontSelectorUpdatesPromptAndTranscriptFonts() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.selectFontFamilyForTest(Font.SERIF);
                frame.selectFontSizeForTest(28);

                assertEquals(Font.SERIF, frame.promptFontForTest().getFamily());
                assertEquals(28, frame.promptFontForTest().getSize());
                assertEquals(frame.promptFontForTest(), frame.transcriptFontForTest());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void zoomInAndOutStepThroughFontSizesAndResetPreservesFamily() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.selectFontFamilyForTest(Font.SERIF);
                assertEquals(18, frame.promptFontForTest().getSize());

                frame.zoomIn();
                assertEquals(20, frame.promptFontForTest().getSize());

                frame.zoomOut();
                frame.zoomOut();
                assertEquals(16, frame.promptFontForTest().getSize());

                frame.zoomReset();
                assertEquals(18, frame.promptFontForTest().getSize());
                assertEquals(Font.SERIF, frame.promptFontForTest().getFamily());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void zoomStopsCleanlyAtConfiguredBounds() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                for (int i = 0; i < 20; i++) {
                    frame.zoomOut();
                }
                assertEquals(14, frame.promptFontForTest().getSize());

                for (int i = 0; i < 20; i++) {
                    frame.zoomIn();
                }
                assertEquals(48, frame.promptFontForTest().getSize());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void detachAndReattachTranscriptPreservesContent() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("reply\n", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.selectBackend("claude");
                frame.setPromptText("hello");
                frame.submitForTest();
            });
            waitForStatus(frame, "Ready");

            onEdt(() -> {
                assertFalse(frame.transcriptDetached());

                frame.detachTranscript();
                assertTrue(frame.transcriptDetached());
                assertTrue(frame.transcriptText().contains("hello"));

                frame.reattachTranscript();
                assertFalse(frame.transcriptDetached());
                assertTrue(frame.transcriptText().contains("hello"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void requiredActionsExistAndExposePresentationMetadata() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                for (String name : new String[] {
                        "sendPrompt",
                        "clearTranscript",
                        "detachTranscript",
                        "copyTranscript",
                        "focusPrompt",
                        "keyboardShortcuts",
                        "about",
                        "zoomIn",
                        "zoomOut",
                        "zoomReset"
                }) {
                    Action action = frame.actionForTest(name);
                    assertNotNull(action);
                    assertNotNull(action.getValue(Action.NAME));
                }
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void keyboardMappingsResolveToSharedActions() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertEquals("sendPrompt", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK)));
                assertEquals("detachTranscript", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK)));
                assertEquals("focusPrompt", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK)));
                assertEquals("copyTranscript", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
                assertEquals("keyboardShortcuts", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)));
                assertEquals("zoomIn", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK)));
                assertEquals("zoomOut", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK)));
                assertEquals("zoomReset", frame.keyBindingForTest(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK)));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void actionEnabledStateFollowsRequestState() throws Exception {
        BlockingBackend backend = new BlockingBackend("response\n");
        MyClawDesktopFrame frame = frameWith("claude", backend);
        try {
            onEdt(() -> {
                assertFalse(frame.actionForTest("sendPrompt").isEnabled());
                assertTrue(frame.actionForTest("clearTranscript").isEnabled());
                frame.setPromptText("hello");
                assertTrue(frame.actionForTest("sendPrompt").isEnabled());
                frame.submitForTest();
            });
            assertTrue(backend.started.await(5, TimeUnit.SECONDS));

            onEdt(() -> {
                assertFalse(frame.actionForTest("sendPrompt").isEnabled());
                assertFalse(frame.actionForTest("clearTranscript").isEnabled());
            });

            backend.finish.countDown();
            waitForStatus(frame, "Ready");

            onEdt(() -> {
                assertFalse(frame.actionForTest("sendPrompt").isEnabled());
                assertTrue(frame.actionForTest("clearTranscript").isEnabled());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void zoomMenuItemsReuseExistingActions() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertSame(frame.actionForTest("zoomIn"), frame.menuActionForTest("View", "Zoom In"));
                assertSame(frame.actionForTest("zoomOut"), frame.menuActionForTest("View", "Zoom Out"));
                assertSame(frame.actionForTest("zoomReset"), frame.menuActionForTest("View", "Reset Text Size"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void detachUpdatesStatusText() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.detachTranscript();
                assertEquals("Transcript detached", frame.statusText());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void reattachUpdatesStatusText() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.detachTranscript();
                frame.reattachTranscript();
                assertEquals("Transcript reattached", frame.statusText());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void backendFontAndSizeLabelsAreAssociatedWithTheirControls() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertTrue(frame.labelAssociatedForTest("backend"));
                assertTrue(frame.labelAssociatedForTest("font"));
                assertTrue(frame.labelAssociatedForTest("size"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void majorControlsHaveMeaningfulAccessibleDescriptions() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                for (String component : new String[] {
                        "backend", "font", "size", "transcript", "prompt", "send", "clear", "status", "progress"
                }) {
                    String description = frame.accessibleDescriptionForTest(component);
                    assertNotNull(description);
                    assertFalse(description.isBlank());
                }
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void sendAccessibleStateReflectsWorkingAndRestoresAfter() throws Exception {
        BlockingBackend backend = new BlockingBackend("response\n");
        MyClawDesktopFrame frame = frameWith("claude", backend);
        try {
            String readyName = onEdtReturning(() -> frame.accessibleNameForTest("send"));
            String readyDescription = onEdtReturning(() -> frame.accessibleDescriptionForTest("send"));

            onEdt(() -> {
                frame.selectBackend("claude");
                frame.setPromptText("hello");
                frame.submitForTest();
            });
            assertTrue(backend.started.await(5, TimeUnit.SECONDS));

            onEdt(() -> {
                assertNotEquals(readyName, frame.accessibleNameForTest("send"));
                assertNotEquals(readyDescription, frame.accessibleDescriptionForTest("send"));
            });

            backend.finish.countDown();
            waitForStatus(frame, "Ready");

            onEdt(() -> {
                assertEquals(readyName, frame.accessibleNameForTest("send"));
                assertEquals(readyDescription, frame.accessibleDescriptionForTest("send"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void aboutDialogUsesCurrentlySelectedFont() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.selectFontFamilyForTest(Font.SERIF);
                frame.selectFontSizeForTest(28);

                Font aboutFont = frame.aboutFontForTest();
                assertEquals(Font.SERIF, aboutFont.getFamily());
                assertEquals(28, aboutFont.getSize());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void plainWheelStillScrollsThePromptArea() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.setPromptText("line\n".repeat(200));
                frame.pack();

                int before = frame.promptScrollValueForTest();
                frame.dispatchPromptWheelEventForTest(false, 3);
                int after = frame.promptScrollValueForTest();

                assertTrue(after > before);
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void ctrlWheelZoomsInsteadOfScrollingThePromptArea() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.setPromptText("line\n".repeat(200));
                frame.pack();
                int startingSize = frame.promptFontForTest().getSize();
                int before = frame.promptScrollValueForTest();

                frame.dispatchPromptWheelEventForTest(true, -1);

                assertEquals(startingSize + 2, frame.promptFontForTest().getSize());
                assertEquals(before, frame.promptScrollValueForTest());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void detachActionTogglesDetachAndReattach() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.actionForTest("detachTranscript").actionPerformed(null);
                assertTrue(frame.transcriptDetached());
                assertEquals("Reattach Transcript", frame.actionForTest("detachTranscript").getValue(Action.NAME));
                assertEquals("Reattach transcript", frame.accessibleNameForTest("reattach"));

                frame.actionForTest("detachTranscript").actionPerformed(null);
                assertFalse(frame.transcriptDetached());
                assertEquals("Detach Transcript", frame.actionForTest("detachTranscript").getValue(Action.NAME));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void focusPromptActionRequestsPromptFocus() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                frame.setVisible(true);
                frame.actionForTest("focusPrompt").actionPerformed(null);
            });
            waitForPromptFocus(frame);
        } finally {
            dispose(frame);
        }
    }

    @Test
    void copyTranscriptCopiesPlainTextThroughClipboardWriter() throws Exception {
        AtomicReference<String> copied = new AtomicReference<>();
        MyClawDesktopFrame frame = frameWithClipboard("claude", request ->
                new AiResponse("reply\n", new BackendId("Claude CLI"), Duration.ZERO),
                copied::set
        );
        try {
            onEdt(() -> {
                frame.selectBackend("claude");
                frame.setPromptText("hello");
                frame.submitForTest();
            });
            waitForStatus(frame, "Ready");

            onEdt(() -> frame.actionForTest("copyTranscript").actionPerformed(null));

            assertEquals(frame.transcriptText(), copied.get());
            assertEquals("Transcript copied", onEdtReturning(frame::statusText));
        } finally {
            dispose(frame);
        }
    }

    @Test
    void clearMenuAndButtonUseSharedAction() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("reply\n", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertSame(frame.actionForTest("clearTranscript"), frame.menuActionForTest("Edit", "Clear Transcript"));
                frame.selectBackend("claude");
                frame.setPromptText("hello");
                frame.submitForTest();
            });
            waitForStatus(frame, "Ready");

            onEdt(() -> {
                frame.menuActionForTest("Edit", "Clear Transcript").actionPerformed(null);
                assertEquals("", frame.transcriptText());
                assertEquals("Transcript cleared", frame.statusText());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void menuItemsReuseIntendedActions() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertSame(frame.actionForTest("copyTranscript"), frame.menuActionForTest("Edit", "Copy Transcript"));
                assertSame(frame.actionForTest("focusPrompt"), frame.menuActionForTest("Edit", "Focus Prompt"));
                assertSame(frame.actionForTest("detachTranscript"), frame.menuActionForTest("View", "Detach Transcript"));
                assertSame(frame.actionForTest("keyboardShortcuts"), frame.menuActionForTest("Help", "Keyboard Shortcuts"));
                assertSame(frame.actionForTest("about"), frame.menuActionForTest("Help", "About myclaw"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void majorControlsHaveAccessibleNames() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                assertEquals("Backend selector", frame.accessibleNameForTest("backend"));
                assertEquals("Font selector", frame.accessibleNameForTest("font"));
                assertEquals("Text size selector", frame.accessibleNameForTest("size"));
                assertEquals("Transcript", frame.accessibleNameForTest("transcript"));
                assertEquals("Prompt editor", frame.accessibleNameForTest("prompt"));
                assertEquals("Send", frame.accessibleNameForTest("send"));
                assertEquals("Clear transcript", frame.accessibleNameForTest("clear"));
                assertEquals("Status", frame.accessibleNameForTest("status"));
                assertEquals("Request progress", frame.accessibleNameForTest("progress"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void helpTextContainsRequiredShortcuts() throws Exception {
        MyClawDesktopFrame frame = frameWith("claude", request ->
                new AiResponse("", new BackendId("Claude CLI"), Duration.ZERO)
        );
        try {
            onEdt(() -> {
                String help = frame.helpTextForTest();
                assertTrue(help.contains("Ctrl+Enter"));
                assertTrue(help.contains("Enter twice"));
                assertTrue(help.contains("Ctrl+D"));
                assertTrue(help.contains("Ctrl+L"));
                assertTrue(help.contains("Ctrl+Shift+C"));
                assertTrue(help.contains("Ctrl++"));
                assertTrue(help.contains("Ctrl+-"));
                assertTrue(help.contains("Ctrl+0"));
                assertTrue(help.contains("F1"));
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void acceptedRequestShowsWorkingStateAndPreventsDuplicateSubmission() throws Exception {
        BlockingBackend backend = new BlockingBackend("response\n");
        MyClawDesktopFrame frame = frameWith("claude", backend);
        try {
            onEdt(() -> {
                frame.selectBackend("claude");
                frame.setPromptText("first prompt");
                frame.submitForTest();
                frame.submitForTest();
            });
            assertTrue(backend.started.await(5, TimeUnit.SECONDS));

            onEdt(() -> {
                assertEquals("Waiting for Claude...", frame.statusText());
                assertFalse(frame.sendEnabled());
                assertFalse(frame.backendSelectionEnabled());
                assertTrue(frame.progressActive());
                assertTrue(frame.requestActive());
                assertEquals(Cursor.WAIT_CURSOR, frame.getCursor().getType());
                assertTrue(frame.transcriptText().contains("You:\nfirst prompt"));
            });
            assertEquals(1, backend.calls.get());

            backend.finish.countDown();
            waitForStatus(frame, "Ready");
        } finally {
            dispose(frame);
        }
    }

    @Test
    void successfulRequestAppendsResponseAndRestoresReadyState() throws Exception {
        BlockingBackend backend = new BlockingBackend("done\n");
        MyClawDesktopFrame frame = frameWith("glm", backend);
        try {
            onEdt(() -> {
                frame.selectBackend("glm");
                frame.setPromptText("Say exactly: done");
                frame.submitForTest();
            });
            assertTrue(backend.started.await(5, TimeUnit.SECONDS));
            backend.finish.countDown();
            waitForStatus(frame, "Ready");

            onEdt(() -> {
                assertTrue(frame.transcriptText().contains("Ollama glm4:9b:\ndone\n"));
                assertFalse(frame.progressActive());
                assertTrue(frame.backendSelectionEnabled());
                assertFalse(frame.requestActive());
                assertEquals(Cursor.DEFAULT_CURSOR, frame.getCursor().getType());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void failedRequestAppendsErrorAndRestoresUsableState() throws Exception {
        FailingBackend backend = new FailingBackend(new AiBackendExecutionException(
                "Claude CLI exited with status 1",
                new BackendId("Claude CLI"),
                new CommandResult(1, "", "authentication required", Duration.ofMillis(5), false)
        ));
        MyClawDesktopFrame frame = frameWith("claude", backend);
        try {
            onEdt(() -> {
                frame.selectBackend("claude");
                frame.setPromptText("fail");
                frame.submitForTest();
            });
            waitForStatus(frame, "Failed");

            onEdt(() -> {
                assertTrue(frame.transcriptText().contains("Claude error:"));
                assertTrue(frame.transcriptText().contains("Claude CLI exited with status 1"));
                assertTrue(frame.transcriptText().contains("stderr:\nauthentication required"));
                assertFalse(frame.progressActive());
                assertTrue(frame.backendSelectionEnabled());
                assertFalse(frame.requestActive());
                assertEquals(Cursor.DEFAULT_CURSOR, frame.getCursor().getType());
            });
        } finally {
            dispose(frame);
        }
    }

    @Test
    void unexpectedBackendRuntimeFailureStillRestoresUsableState() throws Exception {
        FailingBackend backend = new FailingBackend(new IllegalStateException("unexpected failure"));
        MyClawDesktopFrame frame = frameWith("glm", backend);
        try {
            onEdt(() -> {
                frame.selectBackend("glm");
                frame.setPromptText("fail unexpectedly");
                frame.submitForTest();
            });
            waitForStatus(frame, "Failed");

            onEdt(() -> {
                assertTrue(frame.transcriptText().contains("GLM error:"));
                assertTrue(frame.transcriptText().contains("unexpected failure"));
                assertFalse(frame.progressActive());
                assertTrue(frame.backendSelectionEnabled());
                assertFalse(frame.requestActive());
                assertEquals(Cursor.DEFAULT_CURSOR, frame.getCursor().getType());
            });
        } finally {
            dispose(frame);
        }
    }

    private MyClawDesktopFrame frameWith(String backendName, AiBackend backend) throws Exception {
        return frameWithClipboard(backendName, backend, text -> {
        });
    }

    private MyClawDesktopFrame frameWithClipboard(
            String backendName,
            AiBackend backend,
            MyClawDesktopFrame.ClipboardWriter clipboardWriter
    ) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC);
        PromptService service = new PromptService(
                Map.of(backendName, backend),
                new TranscriptWriter(tempDir, clock),
                clock
        );
        return onEdtReturning(() -> new MyClawDesktopFrame(service, new ThemeManager(), clipboardWriter));
    }

    private static void waitForStatus(MyClawDesktopFrame frame, String status) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onEdtReturning(() -> frame.statusText().equals(status))) {
                return;
            }
            Thread.sleep(25);
        }
        assertEquals(status, onEdtReturning(frame::statusText));
    }

    private static void waitForPromptFocus(MyClawDesktopFrame frame) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onEdtReturning(frame::promptFocusOwnerForTest)) {
                return;
            }
            Thread.sleep(25);
        }
        assertTrue(onEdtReturning(frame::promptFocusOwnerForTest));
    }

    private static void dispose(MyClawDesktopFrame frame) throws Exception {
        onEdt(frame::dispose);
    }

    private static void onEdt(ThrowingRunnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    action.run();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException && runtimeException.getCause() instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private static <T> T onEdtReturning(ThrowingSupplier<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.get();
        }
        Holder<T> holder = new Holder<>();
        onEdt(() -> holder.value = action.get());
        return holder.value;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class BlockingBackend implements AiBackend {
        private final String responseText;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch finish = new CountDownLatch(1);
        private final AtomicInteger calls = new AtomicInteger();

        private BlockingBackend(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            calls.incrementAndGet();
            started.countDown();
            try {
                assertTrue(finish.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
            return new AiResponse(responseText, new BackendId("Ollama glm4:9b"), Duration.ofMillis(10));
        }
    }

    private static final class FailingBackend implements AiBackend {
        private final RuntimeException failure;

        private FailingBackend(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            throw failure;
        }
    }
}
