package net.vibmc.network;

import net.vibmc.player.PlayerConnection;

public interface ServerMessageHandler {
    void handle(PlayerConnection connection, String message);
}
