package myclaw.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A tiny loopback-only HTTP server that serves the most recent claude.ai web
 * conversation as role-prefixed Markdown on {@code GET /latest-chat}.
 *
 * It carries no Playwright dependency itself: the transcript is produced by a
 * supplier handed in by the caller. Production wires that supplier to
 * {@link PlaywrightWebAdapter#latestClaudeChatMarkdown()}; tests hand in a stub.
 *
 * Response contract (matches what ChatMap's {@code HttpChatProvider} expects):
 * <ul>
 *   <li>transcript present  -&gt; 200 with {@code text/markdown} body</li>
 *   <li>no conversation     -&gt; 204 No Content (supplier returned empty/blank)</li>
 *   <li>genuine failure     -&gt; 502 with a short {@code text/plain} explanation</li>
 *   <li>wrong method/path   -&gt; 405 / 404</li>
 * </ul>
 * "Browser not running / not logged in / no conversations" is expressed by the
 * supplier returning {@link Optional#empty()} (yielding 204), never by throwing;
 * throwing is reserved for genuine faults (CDP unreachable, page never loaded).
 */
public final class ClaudeWebLatestServer implements AutoCloseable {

    public static final String PATH = "/latest-chat";

    private final HttpServer server;
    private final Supplier<Optional<String>> transcriptSupplier;

    /**
     * @param port bind port; 0 selects an ephemeral free port (useful in tests).
     * @param transcriptSupplier returns the latest transcript as Markdown, empty
     *        when nothing is available; may throw to signal a genuine failure.
     */
    public ClaudeWebLatestServer(int port, Supplier<Optional<String>> transcriptSupplier) throws IOException {
        this.transcriptSupplier = Objects.requireNonNull(transcriptSupplier, "transcriptSupplier");
        this.server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext(PATH, this::handle);
        server.setExecutor(null); // default executor is fine for a single-user local endpoint
    }

    public ClaudeWebLatestServer start() {
        server.start();
        return this;
    }

    /** The actual bound port (meaningful after construction, including when 0 was requested). */
    public int port() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writePlain(exchange, 405, "Only GET is supported.");
                return;
            }

            Optional<String> markdown;
            try {
                markdown = transcriptSupplier.get();
            } catch (Exception failure) {
                writePlain(exchange, 502,
                        "Could not read the latest claude.ai chat: " + describe(failure));
                return;
            }

            if (markdown == null || markdown.isEmpty() || markdown.get().isBlank()) {
                exchange.sendResponseHeaders(204, -1); // No Content: nothing to summarize
                return;
            }

            byte[] body = markdown.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/markdown; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } finally {
            exchange.close();
        }
    }

    private static void writePlain(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String describe(Throwable failure) {
        String message = failure.getMessage();
        return (message == null || message.isBlank())
                ? failure.getClass().getSimpleName()
                : message;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
