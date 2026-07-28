package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.server.VibMC;
import net.vibmc.server.util.Logger;

public class StatusHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId == 0x00) {
            sendStatusResponse(connection);
        } else if (packetId == 0x01) {
            handlePing(connection, buffer);
        }
    }

    private void sendStatusResponse(ClientConnection connection) {
        var config = VibMC.getInstance().getConfig();
        var server = VibMC.getInstance();
        int onlineCount = server.getNetworkServer().getOnlineCount();
        int maxPlayers = config.getMaxPlayers();

        String json = String.format(
            "{\"version\":{\"name\":\"vib-MC 1.12.2\",\"protocol\":340}," +
            "\"players\":{\"max\":%d,\"online\":%d,\"sample\":[]}," +
            "\"description\":{\"text\":\"%s\"}," +
            "\"enforcesSecureChat\":false}",
            maxPlayers, onlineCount, config.getMotd()
        );

        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x00; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) { b.writeString(json); }
        });
    }

    private void handlePing(ClientConnection connection, PacketBuffer buffer) {
        long payload = buffer.readLong();
        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x01; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) { b.writeLong(payload); }
        });
    }

    @Override
    public void onDisconnect(ClientConnection connection, String reason) {
    }

    @Override
    public void onConnect(ClientConnection connection) {
    }

    private interface Packet extends net.vibmc.network.Packet {}
}
