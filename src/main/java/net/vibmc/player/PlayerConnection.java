package net.vibmc.player;

public interface PlayerConnection {
    Player player();

    void send(String message);

    void disconnect(String reason);
}
