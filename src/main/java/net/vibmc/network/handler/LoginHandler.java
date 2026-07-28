package net.vibmc.network.handler;

import net.vibmc.entity.Entity;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.network.*;
import net.vibmc.player.PlayerManager;
import net.vibmc.server.VibMC;
import net.vibmc.server.util.Logger;
import net.vibmc.server.util.UUIDUtil;

import java.util.UUID;

public class LoginHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId == 0x00) {
            handleLoginStart(connection, buffer);
        }
    }

    private void handleLoginStart(ClientConnection connection, PacketBuffer buffer) {
        String username = buffer.readString();
        connection.setUsername(username);

        var server = VibMC.getInstance();
        if (server.getNetworkServer().getOnlineCount() >= server.getConfig().getMaxPlayers()) {
            disconnect(connection, "{\"text\":\"Server is full!\"}");
            return;
        }

        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x02; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeString(connection.getUuid().toString());
                b.writeString(connection.getUsername());
            }
        });

        connection.setState(ProtocolState.PLAY);
        connection.setHandler(new PlayHandler());
        PlayerEntity player = new PlayerEntity(connection);
        server.getPlayerManager().addPlayer(player);
    }

    private void handleLoginAcknowledged(ClientConnection connection) {
    }

    private void disconnect(ClientConnection connection, String reason) {
        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x00; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) { b.writeString(reason); }
        });
        connection.disconnect(reason);
    }

    @Override
    public void onDisconnect(ClientConnection connection, String reason) {
    }

    @Override
    public void onConnect(ClientConnection connection) {
    }

    private interface Packet extends net.vibmc.network.Packet {}
}
