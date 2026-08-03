package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.Packet;
import net.vibmc.network.PacketBuffer;
import net.vibmc.server.ServerConfig;
import net.vibmc.server.VibMC;

public class StatusHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId == 0x00) {
            String json = buildStatusJson();
            connection.sendPacket(new Packet() {
                @Override
                public int getPacketId() {
                    return 0x00;
                }

                @Override
                public void read(PacketBuffer b) {
                }

                @Override
                public void write(PacketBuffer b) {
                    b.writeString(json);
                }
            });
        } else if (packetId == 0x01) {
            long time = buffer.readLong();
            connection.sendPacket(new Packet() {
                @Override
                public int getPacketId() {
                    return 0x01;
                }

                @Override
                public void read(PacketBuffer b) {
                }

                @Override
                public void write(PacketBuffer b) {
                    b.writeLong(time);
                }
            });
        }
    }

    private String buildStatusJson() {
        VibMC server = VibMC.getInstance();
        ServerConfig config = server.getConfig();
        int online = server.getPlayerManager().getOnlineCount();
        return "{\"version\":{\"name\":\"1.12.2\",\"protocol\":340},"
                + "\"players\":{\"max\":" + config.maxPlayers() + ",\"online\":" + online + ",\"sample\":[]},"
                + "\"description\":{\"text\":\"" + config.motd() + "\"}}";
    }
}
