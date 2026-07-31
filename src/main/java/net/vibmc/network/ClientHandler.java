package net.vibmc.network;

import net.vibmc.server.Server;
import net.vibmc.player.Player;
import net.vibmc.player.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {
    private final Server server;
    private final Socket socket;
    private final Map<String, String> state = new HashMap<>();
    private final AtomicBoolean connected = new AtomicBoolean(true);

    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[1024];
            while (!socket.isClosed()) {
                int read = in.read(buffer);
                if (read < 0) {
                    break;
                }
                String message = new String(buffer, 0, read, StandardCharsets.UTF_8);
                if (message.contains("ping")) {
                    out.writeUTF("pong");
                    out.flush();
                }
                if (message.contains("status")) {
                    out.writeUTF("{\"motd\":\"" + server.config().motd() + "\",\"online\":" + server.playerManager().connections().size() + "}");
                    out.flush();
                }
                if (message.startsWith("login")) {
                    String name = parseLoginName(message);
                    if (name != null && !name.isBlank()) {
                        state.put("logged-in", "true");
                        state.put("player-name", name);
                        Player player = new Player(name);
                        PlayerConnection connection = new SocketPlayerConnection(player, out);
                        out.writeUTF("login-ok");
                        out.flush();
                        server.joinPlayer(connection);
                    } else {
                        out.writeUTF("login-failed");
                        out.flush();
                    }
                }
                if (message.contains("chat")) {
                    out.writeUTF("chat-ok");
                    out.flush();
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (connected.compareAndSet(true, false)) {
                String playerName = state.get("player-name");
                PlayerConnection connection = null;
                if (playerName != null) {
                    connection = server.playerManager().get(playerName);
                }
                if (connection != null) {
                    server.disconnectPlayer(connection, "Connection closed");
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String parseLoginName(String message) {
        String[] parts = message.split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }
        return parts[1].trim();
    }

    private static final class SocketPlayerConnection implements PlayerConnection {
        private final Player player;
        private final DataOutputStream output;

        private SocketPlayerConnection(Player player, DataOutputStream output) {
            this.player = player;
            this.output = output;
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public void send(String message) {
            try {
                output.writeUTF(message);
                output.flush();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void disconnect(String reason) {
            send("DISCONNECT:" + reason);
        }
    }
}
