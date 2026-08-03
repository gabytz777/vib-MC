package net.vibmc.network.handler;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.network.ClientConnection;
import net.vibmc.network.Packet;
import net.vibmc.network.PacketBuffer;
import net.vibmc.network.ProtocolState;
import net.vibmc.server.VibMC;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LoginHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        if (packetId != 0x00) {
            return;
        }
        String username = buffer.readString();
        connection.setUsername(username);
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));

        connection.sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x02; // Login Success
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeString(uuid.toString());
                b.writeString(username);
            }
        });

        connection.setProtocolState(ProtocolState.PLAY);

        VibMC server = VibMC.getInstance();
        PlayerEntity player = new PlayerEntity(
                server.getWorldManager().getMainWorld(), connection, username, uuid);
        player.spawnAtSpawn();
        connection.setHandler(new PlayHandler(player));
        server.getPlayerManager().addPlayer(player);
        server.getWorldManager().getMainWorld().addEntity(player);
    }
}
