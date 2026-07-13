package myclaw.transport.socket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import myclaw.application.PromptService;
import myclaw.transport.socket.SocketFrameReader.RequestTooLargeException;

public final class MyClawSocketServer implements AutoCloseable {
    private final PromptService promptService;
    private final SocketServerConfig config;
    private final List<Socket> clientSockets = new ArrayList<>();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public MyClawSocketServer(PromptService promptService, SocketServerConfig config) {
        this.promptService = Objects.requireNonNull(promptService, "promptService");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), config.port()));
        running = true;
        acceptThread = new Thread(this::acceptConnections, "myclaw-socket-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int port() {
        if (serverSocket == null) {
            throw new IllegalStateException("Socket server has not been started");
        }
        return serverSocket.getLocalPort();
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout((int) config.idleTimeout().toMillis());
                synchronized (clientSockets) {
                    clientSockets.add(socket);
                }
                Thread connectionThread = new Thread(
                        () -> handleConnection(socket),
                        "myclaw-socket-client-" + socket.getPort()
                );
                connectionThread.setDaemon(true);
                connectionThread.start();
            } catch (SocketException exception) {
                if (running) {
                    SocketTransportLog.error("Socket accept failed.", exception);
                }
            } catch (IOException exception) {
                if (running) {
                    SocketTransportLog.error("Socket accept failed.", exception);
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        SocketJsonProtocol protocol = new SocketJsonProtocol(promptService);
        try (socket;
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            SocketFrameReader reader = new SocketFrameReader(socket.getInputStream(), config.maxFrameBytes());
            while (running && !socket.isClosed()) {
                String frame;
                try {
                    frame = reader.readFrame();
                } catch (RequestTooLargeException exception) {
                    writeFrame(writer, protocol.requestTooLarge());
                    break;
                }
                if (frame == null) {
                    break;
                }

                SocketJsonProtocol.ProtocolResult result = protocol.handle(frame);
                writeFrame(writer, result.response());
                if (result.closeConnection()) {
                    break;
                }
            }
        } catch (SocketException exception) {
            if (running) {
                SocketTransportLog.error("Socket client connection failed.", exception);
            }
        } catch (IOException exception) {
            if (running) {
                SocketTransportLog.error("Socket client connection failed.", exception);
            }
        } finally {
            synchronized (clientSockets) {
                clientSockets.remove(socket);
            }
        }
    }

    private static void writeFrame(BufferedWriter writer, String response) throws IOException {
        writer.write(response);
        writer.write('\n');
        writer.flush();
    }

    @Override
    public void close() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException exception) {
                SocketTransportLog.error("Could not close socket server.", exception);
            }
        }
        synchronized (clientSockets) {
            for (Socket socket : List.copyOf(clientSockets)) {
                try {
                    socket.close();
                } catch (IOException exception) {
                    SocketTransportLog.error("Could not close socket client.", exception);
                }
            }
            clientSockets.clear();
        }
    }
}
