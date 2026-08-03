package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.network.ProtocolState;

public class HandshakeHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId == 0x00) {
            buffer.readVarInt(); // protocol version
            buffer.readString(); // server address
            buffer.readUnsignedShort(); // server port
            int nextState = buffer.readVarInt();
            if (nextState == 1) {
                connection.setProtocolState(ProtocolState.STATUS);
                connection.setHandler(new StatusHandler());
            } else if (nextState == 2) {
                connection.setProtocolState(ProtocolState.LOGIN);
                connection.setHandler(new LoginHandler());
            }
        }
        // Legacy server list ping (0xFE) is ignored.
    }
}
