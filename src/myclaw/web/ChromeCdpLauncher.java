package myclaw.web;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Gets a CDP-reachable Chrome running for the claude.ai endpoint, so the manual
 * {@code start-chrome-cdp-claude.sh} step is not required.
 *
 * This launches ONE level above {@link PlaywrightWebAdapter}: it is invoked once
 * at server startup from {@code main}. It does not touch the adapter's
 * {@code connectViaCdpOnly()} contract (never launch, just return false) — by
 * the time any request reaches the adapter, Chrome is already (being) launched.
 *
 * The launch mirrors {@code start-chrome-cdp-claude.sh} exactly: same binary
 * resolution and the same persistent profile dir ({@code ~/.chrome-cdp-profile}),
 * so a login done through either path is shared.
 */
public class ChromeCdpLauncher {

    static final String PROFILE_DIR_NAME = ".chrome-cdp-profile";
    private static final int DEFAULT_CDP_PORT = 9222;

    private final Duration readyTimeout;
    private final Duration pollInterval;
    private final HttpClient httpClient;

    public ChromeCdpLauncher() {
        this(Duration.ofSeconds(8), Duration.ofMillis(300));
    }

    public ChromeCdpLauncher(Duration readyTimeout, Duration pollInterval) {
        this.readyTimeout = readyTimeout;
        this.pollInterval = pollInterval;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Ensures a CDP-reachable Chrome is (coming) up. If one already answers on
     * the CDP port, does nothing. Otherwise launches Chrome pointed at claude.ai
     * with the persistent profile and polls until it answers or the timeout
     * elapses. Returns true if CDP is reachable by the deadline; a false return
     * is not fatal — the endpoint returns 204 until Chrome finishes coming up.
     */
    public boolean ensureChromeRunning(String cdpUrl, PrintStream out) {
        if (isCdpAvailable(cdpUrl)) {
            return true; // already running (e.g. via start-chrome-cdp-claude.sh) -> no second window
        }

        int cdpPort = portOf(cdpUrl);
        out.println("Chrome not found on port " + cdpPort + ", launching...");
        try {
            launchChrome(cdpPort);
        } catch (IOException failure) {
            out.println("Could not launch Chrome automatically: " + failure.getMessage()
                    + " (start it by hand with start-chrome-cdp-claude.sh)");
            return false;
        }
        return waitForCdp(cdpUrl, out);
    }

    /** GET {@code <cdpUrl>/json/version}; true when Chrome answers 2xx. */
    public boolean isCdpAvailable(String cdpUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(versionProbeUrl(cdpUrl)))
                    .timeout(Duration.ofMillis(800))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() / 100 == 2;
        } catch (IOException | InterruptedException | RuntimeException notReachable) {
            if (notReachable instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /** Launches Chrome exactly as the shell script does. Package-visible so tests can override. */
    void launchChrome(int cdpPort) throws IOException {
        new ProcessBuilder(
                resolveChromeBinary(),
                "--remote-debugging-port=" + cdpPort,
                "--user-data-dir=" + profileDir(),
                PlaywrightWebAdapter.CLAUDE_BASE_URL)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private boolean waitForCdp(String cdpUrl, PrintStream out) {
        long deadline = System.nanoTime() + readyTimeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isCdpAvailable(cdpUrl)) {
                return true;
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        boolean up = isCdpAvailable(cdpUrl);
        if (!up) {
            out.println("Chrome did not answer on the CDP port yet; the endpoint will return 204 "
                    + "until it finishes launching.");
        }
        return up;
    }

    static String versionProbeUrl(String cdpUrl) {
        String base = cdpUrl.endsWith("/") ? cdpUrl.substring(0, cdpUrl.length() - 1) : cdpUrl;
        return base + "/json/version";
    }

    static int portOf(String cdpUrl) {
        try {
            int port = URI.create(cdpUrl).getPort();
            return port == -1 ? DEFAULT_CDP_PORT : port;
        } catch (Exception malformed) {
            return DEFAULT_CDP_PORT;
        }
    }

    /** Same resolution as start-chrome-cdp-claude.sh: two Windows paths, else google-chrome. */
    static String resolveChromeBinary() {
        String[] candidates = {
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
        };
        for (String candidate : candidates) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return candidate;
            }
        }
        return "google-chrome";
    }

    /** The persistent profile the shell script uses: {@code ~/.chrome-cdp-profile}. */
    static String profileDir() {
        return Path.of(System.getProperty("user.home"), PROFILE_DIR_NAME).toString();
    }
}
