package myclaw.transport.socket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import myclaw.application.PromptService;
import myclaw.backend.AiBackend;
import myclaw.backend.AiBackendExecutionException;
import myclaw.backend.AiBackendStartupException;
import myclaw.backend.AiBackendUnsupportedRequestException;
import myclaw.backend.AiRequest;
import myclaw.backend.AiResponse;
import myclaw.backend.BackendId;
import myclaw.backend.PromptProfile;
import myclaw.execution.CommandResult;
import myclaw.transcript.TranscriptWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MyClawSocketServerTest {
    @TempDir
    Path tempDir;

    @Test
    void healthReturnsStatusAndProtocolVersion() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"1\",\"operation\":\"health\"}");

            assertOk(response, "1");
            assertEquals(1, response.get("protocolVersion").getAsInt());
        }
    }

    @Test
    void listBackendsReturnsIdAndLabelOnly() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"2\",\"operation\":\"listBackends\"}");

            assertOk(response, "2");
            JsonArray backends = response.getAsJsonArray("backends");
            assertEquals(1, backends.size());
            JsonObject backend = backends.get(0).getAsJsonObject();
            assertEquals("fake", backend.get("id").getAsString());
            assertEquals("Fake Backend", backend.get("label").getAsString());
            assertEquals(2, backend.size());
        }
    }

    @Test
    void generalChatCallsPromptServiceWithGeneralProfile() throws Exception {
        CapturingBackend backend = new CapturingBackend("answer");
        try (ServerFixture fixture = start(backend)) {
            JsonObject response = fixture.request("""
                    {"requestId":"3","operation":"chat","backendId":"fake","prompt":"Explain recursion."}""");

            assertOk(response, "3");
            assertEquals("fake", response.get("backendId").getAsString());
            assertEquals("answer", response.get("content").getAsString());
            assertEquals(PromptProfile.GENERAL, backend.requests.get(0).profile());
            assertEquals("Explain recursion.", backend.requests.get(0).prompt());
        }
    }

    @Test
    void omittedProfileDefaultsToGeneral() throws Exception {
        CapturingBackend backend = new CapturingBackend("answer");
        try (ServerFixture fixture = start(backend)) {
            fixture.request("{\"requestId\":\"4\",\"operation\":\"chat\",\"backendId\":\"fake\",\"prompt\":\"hello\"}");

            assertEquals(PromptProfile.GENERAL, backend.requests.get(0).profile());
        }
    }

    @Test
    void guidedChatCallsPromptServiceWithGuidedTeachingProfile() throws Exception {
        CapturingBackend backend = new CapturingBackend("lesson");
        try (ServerFixture fixture = start(backend)) {
            JsonObject response = fixture.request("""
                    {"requestId":"5","operation":"chat","backendId":"fake","profile":"guided-teaching","prompt":"Help me understand fractions."}""");

            assertOk(response, "5");
            assertEquals("lesson", response.get("content").getAsString());
            assertEquals(PromptProfile.GUIDED_TEACHING, backend.requests.get(0).profile());
            assertEquals("Help me understand fractions.", backend.requests.get(0).prompt());
        }
    }

    @Test
    void multilinePromptSurvivesFramingAndParsing() throws Exception {
        CapturingBackend backend = new CapturingBackend("answer");
        try (ServerFixture fixture = start(backend)) {
            fixture.request("{\"requestId\":\"6\",\"operation\":\"chat\",\"backendId\":\"fake\",\"prompt\":\"first line\\nsecond line\"}");

            assertEquals("first line\nsecond line", backend.requests.get(0).prompt());
        }
    }

    @Test
    void multilineResponseIsEscapedAndReturnedAsOneFrame() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("first line\nsecond line"))) {
            String line = fixture.requestLine("{\"requestId\":\"7\",\"operation\":\"chat\",\"backendId\":\"fake\",\"prompt\":\"hello\"}");

            assertFalse(line.contains("\n"));
            JsonObject response = parse(line);
            assertEquals("first line\nsecond line", response.get("content").getAsString());
        }
    }

    @Test
    void crlfInputIsAccepted() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("answer"))) {
            JsonObject response = fixture.requestRaw("{\"requestId\":\"8\",\"operation\":\"health\"}\r\n");

            assertOk(response, "8");
        }
    }

    @Test
    void oversizedFrameIsRejectedAndConnectionCloses() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"), 24)) {
            Client client = fixture.client();
            client.writeRaw("{\"requestId\":\"9\",\"operation\":\"health\",\"extra\":\"too large\"}\n");

            JsonObject response = parse(client.readLine());
            assertError(response, null, "request_too_large");
            assertNull(client.readLine());
        }
    }

    @Test
    void malformedJsonReturnsErrorAndClosesConnection() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            Client client = fixture.client();
            client.write("{bad json");

            JsonObject response = parse(client.readLine());
            assertError(response, null, "malformed_json");
            assertNull(client.readLine());
        }
    }

    @Test
    void missingRequestIdIsValidationErrorAndConnectionStaysOpen() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            Client client = fixture.client();
            client.write("{\"operation\":\"health\"}");
            assertError(parse(client.readLine()), null, "validation_error");

            client.write("{\"requestId\":\"after\",\"operation\":\"health\"}");
            assertOk(parse(client.readLine()), "after");
        }
    }

    @Test
    void blankRequestIdIsValidationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"\",\"operation\":\"health\"}");

            assertError(response, "", "validation_error");
        }
    }

    @Test
    void missingOperationIsValidationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"10\"}");

            assertError(response, "10", "validation_error");
        }
    }

    @Test
    void unknownOperationIsUnknownOperationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"11\",\"operation\":\"dance\"}");

            assertError(response, "11", "unknown_operation");
        }
    }

    @Test
    void missingChatBackendIdIsValidationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"12\",\"operation\":\"chat\",\"prompt\":\"hello\"}");

            assertError(response, "12", "validation_error");
        }
    }

    @Test
    void missingChatPromptIsValidationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"13\",\"operation\":\"chat\",\"backendId\":\"fake\"}");

            assertError(response, "13", "validation_error");
        }
    }

    @Test
    void blankChatPromptIsValidationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("{\"requestId\":\"14\",\"operation\":\"chat\",\"backendId\":\"fake\",\"prompt\":\"   \"}");

            assertError(response, "14", "validation_error");
        }
    }

    @Test
    void unknownProfileIsValidationError() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("""
                    {"requestId":"15","operation":"chat","backendId":"fake","profile":"wizard","prompt":"hello"}""");

            assertError(response, "15", "validation_error");
        }
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("answer"))) {
            JsonObject response = fixture.request("""
                    {"requestId":"16","operation":"chat","backendId":"fake","prompt":"hello","future":true}""");

            assertOk(response, "16");
        }
    }

    @Test
    void unknownBackendMapsToUnknownBackend() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("unused"))) {
            JsonObject response = fixture.request("""
                    {"requestId":"17","operation":"chat","backendId":"missing","prompt":"hello"}""");

            assertError(response, "17", "unknown_backend");
        }
    }

    @Test
    void startupFailureMapsToBackendUnavailable() throws Exception {
        CapturingBackend backend = new CapturingBackend(new AiBackendStartupException(
                "secret startup details", new BackendId("Fake Backend"), new RuntimeException("root")));
        try (ServerFixture fixture = start(backend)) {
            JsonObject response = fixture.request("""
                    {"requestId":"18","operation":"chat","backendId":"fake","prompt":"hello"}""");

            assertError(response, "18", "backend_unavailable");
            assertFalse(response.toString().contains("secret startup details"));
        }
    }

    @Test
    void executionFailureMapsToBackendFailed() throws Exception {
        CapturingBackend backend = new CapturingBackend(new AiBackendExecutionException(
                "secret execution details",
                new BackendId("Fake Backend"),
                new CommandResult(1, "", "secret stderr", Duration.ofMillis(1), false)));
        try (ServerFixture fixture = start(backend)) {
            JsonObject response = fixture.request("""
                    {"requestId":"19","operation":"chat","backendId":"fake","prompt":"hello"}""");

            assertError(response, "19", "backend_failed");
            assertFalse(response.toString().contains("secret stderr"));
        }
    }

    @Test
    void unsupportedRequestMapsToUnsupportedRequest() throws Exception {
        CapturingBackend backend = new CapturingBackend(new AiBackendUnsupportedRequestException(
                "unsupported details", new BackendId("Fake Backend")));
        try (ServerFixture fixture = start(backend)) {
            JsonObject response = fixture.request("""
                    {"requestId":"20","operation":"chat","backendId":"fake","prompt":"hello"}""");

            assertError(response, "20", "unsupported_request");
        }
    }

    @Test
    void unexpectedExceptionBecomesGenericInternalError() throws Exception {
        CapturingBackend backend = new CapturingBackend(new IllegalStateException("secret bug"));
        try (ServerFixture fixture = start(backend)) {
            JsonObject response = fixture.request("""
                    {"requestId":"21","operation":"chat","backendId":"fake","prompt":"hello"}""");

            assertError(response, "21", "internal_error");
            assertFalse(response.toString().contains("secret bug"));
        }
    }

    @Test
    void twoSequentialRequestsWorkOverOneConnection() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("answer"))) {
            Client client = fixture.client();
            client.write("{\"requestId\":\"22\",\"operation\":\"health\"}");
            client.write("{\"requestId\":\"23\",\"operation\":\"listBackends\"}");

            assertOk(parse(client.readLine()), "22");
            assertOk(parse(client.readLine()), "23");
        }
    }

    @Test
    void validRequestWorksAfterValidJsonValidationFailure() throws Exception {
        try (ServerFixture fixture = start(new CapturingBackend("answer"))) {
            Client client = fixture.client();
            client.write("{\"requestId\":\"24\",\"operation\":\"chat\",\"backendId\":\"fake\"}");
            assertError(parse(client.readLine()), "24", "validation_error");

            client.write("{\"requestId\":\"25\",\"operation\":\"health\"}");
            assertOk(parse(client.readLine()), "25");
        }
    }

    @Test
    void oneConnectionProcessesRequestsSequentially() throws Exception {
        BlockingBackend backend = new BlockingBackend();
        try (ServerFixture fixture = start(backend)) {
            Client client = fixture.client();
            client.socket.setSoTimeout(250);

            client.write("{\"requestId\":\"26\",\"operation\":\"chat\",\"backendId\":\"fake\",\"prompt\":\"wait\"}");
            client.write("{\"requestId\":\"27\",\"operation\":\"health\"}");
            assertTrue(backend.started.await(5, TimeUnit.SECONDS));
            assertThrows(SocketTimeoutException.class, client::readLine);

            backend.finish.countDown();
            client.socket.setSoTimeout(5_000);
            assertOk(parse(client.readLine()), "26");
            assertOk(parse(client.readLine()), "27");
        }
    }

    private ServerFixture start(AiBackend backend) throws Exception {
        return start(backend, SocketServerConfig.DEFAULT_MAX_FRAME_BYTES);
    }

    private ServerFixture start(AiBackend backend, int maxFrameBytes) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T12:00:00Z"), ZoneOffset.UTC);
        PromptService service = new PromptService(
                Map.of("fake", backend),
                Map.of("fake", "Fake Backend"),
                new TranscriptWriter(tempDir, clock),
                clock
        );
        MyClawSocketServer server = new MyClawSocketServer(
                service,
                new SocketServerConfig(0, maxFrameBytes, Duration.ofSeconds(30))
        );
        server.start();
        return new ServerFixture(server);
    }

    private static JsonObject parse(String line) {
        return JsonParser.parseString(line).getAsJsonObject();
    }

    private static void assertOk(JsonObject response, String requestId) {
        assertEquals(requestId, response.get("requestId").getAsString());
        assertEquals("ok", response.get("status").getAsString());
    }

    private static void assertError(JsonObject response, String requestId, String code) {
        if (requestId == null) {
            assertTrue(response.get("requestId").isJsonNull());
        } else {
            assertEquals(requestId, response.get("requestId").getAsString());
        }
        assertEquals("error", response.get("status").getAsString());
        assertEquals(code, response.getAsJsonObject("error").get("code").getAsString());
    }

    private static final class ServerFixture implements AutoCloseable {
        private final MyClawSocketServer server;

        private ServerFixture(MyClawSocketServer server) {
            this.server = server;
        }

        private JsonObject request(String json) throws Exception {
            return parse(requestLine(json));
        }

        private JsonObject requestRaw(String frame) throws Exception {
            try (Client client = client()) {
                client.writeRaw(frame);
                return parse(client.readLine());
            }
        }

        private String requestLine(String json) throws Exception {
            try (Client client = client()) {
                client.write(json);
                return client.readLine();
            }
        }

        private Client client() throws IOException {
            return new Client(new Socket("127.0.0.1", server.port()));
        }

        @Override
        public void close() {
            server.close();
        }
    }

    private static final class Client implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        private Client(Socket socket) throws IOException {
            this.socket = socket;
            this.socket.setSoTimeout(5_000);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private void write(String json) throws IOException {
            writeRaw(json + "\n");
        }

        private void writeRaw(String frame) throws IOException {
            writer.write(frame);
            writer.flush();
        }

        private String readLine() throws IOException {
            return reader.readLine();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private static class CapturingBackend implements AiBackend {
        private final String responseText;
        private final RuntimeException failure;
        private final List<AiRequest> requests = new ArrayList<>();

        private CapturingBackend(String responseText) {
            this.responseText = responseText;
            this.failure = null;
        }

        private CapturingBackend(RuntimeException failure) {
            this.responseText = null;
            this.failure = failure;
        }

        @Override
        public AiResponse ask(AiRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
            }
            return new AiResponse(responseText, new BackendId("Fake Backend"), Duration.ofMillis(1));
        }
    }

    private static final class BlockingBackend extends CapturingBackend {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch finish = new CountDownLatch(1);

        private BlockingBackend() {
            super("blocked");
        }

        @Override
        public AiResponse ask(AiRequest request) {
            started.countDown();
            try {
                assertTrue(finish.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
            return super.ask(request);
        }
    }
}
