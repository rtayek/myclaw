package myclaw.performance;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helper for the latency tests: runs warmups (untimed), then timed
 * runs, and reports min / avg / max plus each run. Also appends a CSV line
 * per timed run to latency-history.csv so you can watch trends over time.
 */
final class LatencyStats {

    private final String label;
    private final List<Long> millis;

    private LatencyStats(String label, List<Long> millis) {
        this.label = label;
        this.millis = millis;
    }

    static LatencyStats measure(String label, int warmups, int runs, Runnable action) {
        for (int i = 0; i < warmups; i++) {
            action.run();
        }
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            action.run();
            times.add((System.nanoTime() - start) / 1_000_000);
        }
        LatencyStats stats = new LatencyStats(label, times);
        stats.appendHistory();
        return stats;
    }

    long averageMillis() {
        return Math.round(millis.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    long minMillis() {
        return millis.stream().mapToLong(Long::longValue).min().orElse(0);
    }

    long maxMillis() {
        return millis.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(label).append(" ===\n");
        for (int i = 0; i < millis.size(); i++) {
            sb.append(String.format("  run %2d: %6d ms%n", i + 1, millis.get(i)));
        }
        sb.append(String.format("  min %d  avg %d  max %d ms%n", minMillis(), averageMillis(), maxMillis()));
        return sb.toString();
    }

    private void appendHistory() {
        String line = String.format("%s,%s,%d,%d,%d%n",
                java.time.Instant.now(), label, minMillis(), averageMillis(), maxMillis());
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("latency-history.csv"),
                    line,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) {
            // history is best-effort; never fail a test over it
        }
    }
}
