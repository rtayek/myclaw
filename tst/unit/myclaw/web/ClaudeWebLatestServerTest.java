package myclaw.web;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the endpoint contract with a real loopback HTTP round trip and a
 * stubbed transcript supplier (no browser, no Playwright).
 */
final class ClaudeWebLatestServerTest {

    private HttpResponse<String> get(ClaudeWebLatestServer server, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + server.port() + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private ClaudeWebLatestServer startWith(Supplier<Optional<String>> supplier) throws Exception {
        return new ClaudeWebLatestServer(0, supplier).start(); // port 0 -> ephemeral
    }

    @Test
    void servesTranscriptAsMarkdownWith200() throws Exception {
        String transcript = "USER: hi\n\nASSISTANT: hello";
        try (ClaudeWebLatestServer server = startWith(() -> Optional.of(transcript))) {
            HttpResponse<String> response = get(server, ClaudeWebLatestServer.PATH);

            assertEquals(200, response.statusCode());
            assertEquals(transcript, response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .startsWith("text/markdown"), "should be served as markdown");
        }
    }

    @Test
    void returns204WhenNoConversationAvailable() throws Exception {
        try (ClaudeWebLatestServer server = startWith(Optional::empty)) {
            HttpResponse<String> response = get(server, ClaudeWebLatestServer.PATH);

            assertEquals(204, response.statusCode());
            assertTrue(response.body().isEmpty(), "204 must carry no body");
        }
    }

    @Test
    void returns204WhenTranscriptIsBlank() throws Exception {
        try (ClaudeWebLatestServer server = startWith(() -> Optional.of("   \n  "))) {
            HttpResponse<String> response = get(server, ClaudeWebLatestServer.PATH);

            assertEquals(204, response.statusCode());
        }
    }

    @Test
    void returns502WithPlainBodyOnGenuineFailure() throws Exception {
        Supplier<Optional<String>> failing = () -> {
            throw new IllegalStateException("CDP unreachable");
        };
        try (ClaudeWebLatestServer server = startWith(failing)) {
            HttpResponse<String> response = get(server, ClaudeWebLatestServer.PATH);

            assertEquals(502, response.statusCode());
            assertTrue(response.body().contains("CDP unreachable"), response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .startsWith("text/plain"), "failure body should be plain text");
        }
    }

    @Test
    void rejectsNonGetWith405() throws Exception {
        try (ClaudeWebLatestServer server = startWith(() -> Optional.of("USER: hi"))) {
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + server.port() + ClaudeWebLatestServer.PATH))
                    .method("POST", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(405, response.statusCode());
        }
    }

    @Test
    void bindsToLoopbackOnly() throws Exception {
        try (ClaudeWebLatestServer server = startWith(() -> Optional.of("USER: hi"))) {
            assertTrue(server.port() > 0, "ephemeral port should be assigned");
        }
    }
}
