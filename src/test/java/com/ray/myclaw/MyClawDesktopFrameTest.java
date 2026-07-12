package com.ray.myclaw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Cursor;
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

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MyClawDesktopFrameTest {
    @TempDir
    Path tempDir;

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
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T21:00:00.123Z"), ZoneOffset.UTC);
        PromptService service = new PromptService(
                Map.of(backendName, backend),
                new TranscriptWriter(tempDir, clock),
                clock
        );
        return onEdtReturning(() -> new MyClawDesktopFrame(service));
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
