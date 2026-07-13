package myclaw.transport.socket;

import java.time.Duration;
import java.util.Objects;

public record SocketServerConfig(
        int port,
        int maxFrameBytes,
        Duration idleTimeout
) {
    public static final int DEFAULT_MAX_FRAME_BYTES = 1024 * 1024;
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(5);

    public SocketServerConfig {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (maxFrameBytes <= 0) {
            throw new IllegalArgumentException("maxFrameBytes must be positive");
        }
        Objects.requireNonNull(idleTimeout, "idleTimeout");
        if (idleTimeout.isNegative() || idleTimeout.isZero()) {
            throw new IllegalArgumentException("idleTimeout must be positive");
        }
    }

    public static SocketServerConfig onPort(int port) {
        return new SocketServerConfig(port, DEFAULT_MAX_FRAME_BYTES, DEFAULT_IDLE_TIMEOUT);
    }
}
