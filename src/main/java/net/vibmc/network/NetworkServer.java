package net.vibmc.network;

import net.vibmc.server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkServer {
    private static final Logger LOGGER = Logger.getLogger(NetworkServer.class.getName());

    private final Server server;
    private final int port;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public NetworkServer(Server server, int port) {
        this.server = server;
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        executor.submit(this::acceptLoop);
        LOGGER.info("Network server listening on port " + port);
    }

    private void acceptLoop() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                executor.submit(new ClientHandler(server, socket));
            }
        } catch (IOException ex) {
            if (!serverSocket.isClosed()) {
                LOGGER.log(Level.WARNING, "Network accept loop terminated", ex);
            }
        }
    }

    public void shutdown() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        executor.shutdownNow();
    }
}
