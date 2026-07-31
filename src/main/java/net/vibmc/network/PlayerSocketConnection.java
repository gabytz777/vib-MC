package net.vibmc.network;

import net.vibmc.player.Player;
import net.vibmc.player.PlayerConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerSocketConnection implements PlayerConnection, Runnable {
    private static final Logger LOGGER = Logger.getLogger(PlayerSocketConnection.class.getName());

    private final Socket socket;
    private final Player player;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final ServerMessageHandler handler;

    public PlayerSocketConnection(Socket socket, Player player, ServerMessageHandler handler) throws IOException {
        this.socket = socket;
        this.player = player;
        this.handler = handler;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public void run() {
        try {
            while (active.get() && !socket.isClosed()) {
                String message = readMessage();
                if (message == null) {
                    break;
                }
                handler.handle(this, message);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Connection closed for " + player.name(), ex);
        } finally {
            disconnect("Connection closed");
        }
    }

    @Override
    public void send(String message) {
        if (!active.get()) {
            return;
        }
        try {
            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
            output.writeInt(payload.length);
            output.write(payload);
            output.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to send message to " + player.name(), ex);
            disconnect("Write failure");
        }
    }

    @Override
    public void disconnect(String reason) {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        try {
            byte[] payload = ("DISCONNECT:" + reason).getBytes(StandardCharsets.UTF_8);
            output.writeInt(payload.length);
            output.write(payload);
            output.flush();
        } catch (Exception ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String readMessage() throws IOException {
        if (socket.isClosed()) {
            return null;
        }
        int length;
        try {
            length = input.readInt();
        } catch (IOException ex) {
            return null;
        }
        if (length <= 0 || length > 32768) {
            return null;
        }
        byte[] buffer = new byte[length];
        input.readFully(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }
}
