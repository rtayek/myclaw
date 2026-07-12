package myclaw.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * PipeBench - warm round-trip timing through ONE open claude session.
 *
 * Run directly, no build needed (Java 11+ single-file mode):
 *
 *     java PipeBench.java 5
 *
 * The number is how many timed turns (default 5). The session open and one
 * warmup turn are NOT timed. Requires `claude` on PATH.
 */
public final class PipeBench {

    private static final String[] PROMPTS = {
            "Reply with exactly: OK",
            "What is 2+2? Just the number.",
            "Name one primary color. One word.",
            "Reply with exactly: PONG",
            "Capital of France? One word.",
    };

    public static void main(String[] args) throws Exception {
        int turns = args.length > 0 ? Integer.parseInt(args[0]) : 5;

        System.out.println("Opening persistent claude session (not timed)...");
        Process proc = new ProcessBuilder(
                "claude", "-p",
                "--input-format", "stream-json",
                "--output-format", "stream-json",
                "--verbose")
                .redirectErrorStream(false)
                .start();

        Writer stdin = new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8);
        BufferedReader stdout = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));

        // Hands each turn-complete line from the reader thread to the main thread.
        SynchronousQueue<String> turnDone = new SynchronousQueue<>();

        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = stdout.readLine()) != null) {
                    // A finished turn arrives as a JSON line with "type":"result".
                    if (line.contains("\"type\":\"result\"")) {
                        turnDone.put(line);
                    }
                }
            } catch (Exception ignored) {
            }
        });
        reader.setDaemon(true);
        reader.start();

        // Warmup turn - pays first-turn setup, not counted.
        sendPrompt(stdin, "Reply with exactly: READY");
        turnDone.take();
        System.out.println("Session warm. Timing begins.");
        System.out.println();

        List<Long> times = new ArrayList<>();
        for (int i = 0; i < turns; i++) {
            long start = System.nanoTime();
            sendPrompt(stdin, PROMPTS[i % PROMPTS.length]);
            String resultLine = turnDone.take();
            long ms = (System.nanoTime() - start) / 1_000_000;
            times.add(ms);
            System.out.printf("turn %2d: %6d ms   %s%n", i + 1, ms, extractApiMs(resultLine));
        }

        long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
        long avg = Math.round(times.stream().mapToLong(Long::longValue).average().orElse(0));
        System.out.println();
        System.out.println("warm round trip: min " + min + "  avg " + avg + "  max " + max + " ms");

        stdin.close();   // closing stdin tells the CLI to finish and exit
        proc.waitFor();
    }

    private static void sendPrompt(Writer stdin, String text) throws Exception {
        // Minimal JSON escaping - fine for these fixed test prompts.
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        String frame = "{\"type\":\"user\",\"message\":{\"role\":\"user\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"" + escaped + "\"}]}}";
        stdin.write(frame);
        stdin.write("\n");
        stdin.flush();
    }

    /** Pulls duration_api_ms out of the result line without a JSON library. */
    private static String extractApiMs(String line) {
        int at = line.indexOf("\"duration_api_ms\":");
        if (at < 0) return "";
        int start = at + "\"duration_api_ms\":".length();
        int end = start;
        while (end < line.length() && Character.isDigit(line.charAt(end))) end++;
        return "(api: " + line.substring(start, end) + " ms)";
    }
}
