package net.vibmc.network.handler;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.server.VibMC;

public class PlayHandler implements PacketHandler {
    private final PlayerEntity player;

    public PlayHandler(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        switch (packetId) {
            case 0x00: // Teleport Confirm
                buffer.readVarInt();
                break;
            case 0x02: { // Chat Message
                String message = buffer.readString();
                VibMC.getInstance().getPlayerManager().handleChat(player, message);
                break;
            }
            case 0x03: { // Client Status
                int action = buffer.readVarInt();
                if (action == 0) {
                    player.respawn();
                }
                break;
            }
            case 0x04: { // Client Settings
                buffer.readString(); // locale
                buffer.readByte(); // view distance
                buffer.readVarInt(); // chat mode
                buffer.readBoolean(); // chat colors
                buffer.readUnsignedByte(); // displayed skin parts
                buffer.readVarInt(); // main hand
                break;
            }
            case 0x09: // Plugin Message
                buffer.readString(); // channel
                break;
            case 0x0B: // Keep Alive
                buffer.readLong();
                break;
            case 0x0C: // Player (on ground)
                player.setOnGround(buffer.readBoolean());
                break;
            case 0x0D: { // Player Position
                double x = buffer.readDouble();
                double y = buffer.readDouble();
                double z = buffer.readDouble();
                boolean onGround = buffer.readBoolean();
                player.setPosition(x, y, z);
                player.setOnGround(onGround);
                break;
            }
            case 0x0E: { // Player Position And Look
                double x = buffer.readDouble();
                double y = buffer.readDouble();
                double z = buffer.readDouble();
                float yaw = buffer.readFloat();
                float pitch = buffer.readFloat();
                boolean onGround = buffer.readBoolean();
                player.setPositionAndRotation(x, y, z, yaw, pitch);
                player.setOnGround(onGround);
                break;
            }
            case 0x0F: { // Player Look
                float yaw = buffer.readFloat();
                float pitch = buffer.readFloat();
                boolean onGround = buffer.readBoolean();
                player.setRotation(yaw, pitch);
                player.setOnGround(onGround);
                break;
            }
            case 0x1A: // Held Item Change
                player.setHeldItemSlot(buffer.readShort());
                break;
            default:
                break;
        }
    }

    @Override
    public void onDisconnect(ClientConnection connection, String reason) {
        VibMC.getInstance().getPlayerManager().removePlayer(player);
        player.getWorld().removeEntity(player);
    }
}
