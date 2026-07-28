package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;

public interface PacketHandler {
    void handle(ClientConnection connection, int packetId, PacketBuffer buffer);
    void onDisconnect(ClientConnection connection, String reason);
    void onConnect(ClientConnection connection);
}
