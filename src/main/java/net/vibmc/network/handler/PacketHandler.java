package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;

public interface PacketHandler {
    default void onConnect(ClientConnection connection) {
    }

    default void onDisconnect(ClientConnection connection, String reason) {
    }

    void handle(ClientConnection connection, int packetId, PacketBuffer buffer);
}
