package myclaw.transport.socket;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

final class SocketTransportLog {
    private static final Logger LOGGER = System.getLogger("myclaw.transport.socket");

    private SocketTransportLog() {
    }

    static void error(String message, Throwable throwable) {
        LOGGER.log(Level.ERROR, message, throwable);
    }
}
