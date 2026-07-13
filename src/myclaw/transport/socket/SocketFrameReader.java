package myclaw.transport.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class SocketFrameReader {
    private final InputStream input;
    private final int maxFrameBytes;

    SocketFrameReader(InputStream input, int maxFrameBytes) {
        this.input = input;
        this.maxFrameBytes = maxFrameBytes;
    }

    String readFrame() throws IOException, RequestTooLargeException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int next = input.read();
            if (next == -1) {
                return null;
            }
            if (next == '\n') {
                byte[] bytes = buffer.toByteArray();
                int length = bytes.length;
                if (length > 0 && bytes[length - 1] == '\r') {
                    length--;
                }
                return new String(bytes, 0, length, StandardCharsets.UTF_8);
            }
            if (buffer.size() >= maxFrameBytes) {
                throw new RequestTooLargeException();
            }
            buffer.write(next);
        }
    }

    static final class RequestTooLargeException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
