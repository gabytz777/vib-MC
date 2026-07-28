package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.network.ProtocolState;
import net.vibmc.server.VibMC;

public class HandshakeHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId != 0x00) {
            connection.disconnect("Unexpected packet");
            return;
        }
        int protocolVersion = buffer.readVarInt();
        String serverAddress = buffer.readString();
        int serverPort = buffer.readShort();
        int nextState = buffer.readVarInt();

        if (nextState == 1) {
            connection.setState(ProtocolState.STATUS);
            connection.setHandler(new StatusHandler());
        } else if (nextState == 2) {
            connection.setState(ProtocolState.LOGIN);
            connection.setHandler(new LoginHandler());
        } else {
            connection.disconnect("Invalid next state");
        }
    }

    @Override
    public void onDisconnect(ClientConnection connection, String reason) {
    }

    @Override
    public void onConnect(ClientConnection connection) {
    }
}
