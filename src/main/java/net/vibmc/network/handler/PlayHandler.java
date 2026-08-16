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
                if (!isFinitePosition(connection, x, y, z)) {
                    return;
                }
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
                if (!isFinitePosition(connection, x, y, z) || !isFiniteLook(connection, yaw, pitch)) {
                    return;
                }
                player.setPositionAndRotation(x, y, z, yaw, pitch);
                player.setOnGround(onGround);
                break;
            }
            case 0x0F: { // Player Look
                float yaw = buffer.readFloat();
                float pitch = buffer.readFloat();
                boolean onGround = buffer.readBoolean();
                if (!isFiniteLook(connection, yaw, pitch)) {
                    return;
                }
                player.setRotation(yaw, pitch);
                player.setOnGround(onGround);
                break;
            }
            case 0x14: { // Player Digging
                int status = buffer.readVarInt();
                int[] position = buffer.readPosition();
                buffer.readByte(); // face
                // Creative breaks instantly, so the "started digging" packet is the whole
                // event there; survival clients time the dig themselves and report when
                // they are finished.
                boolean creative = player.getGameModeEnum() == net.vibmc.player.GameMode.CREATIVE;
                if (status == 2 || (creative && status == 0)) {
                    net.vibmc.world.BlockInteraction.breakBlock(
                            player, position[0], position[1], position[2]);
                }
                break;
            }
            case 0x1A: // Held Item Change
                player.setHeldItemSlot(buffer.readShort());
                break;
            case 0x1B: { // Creative Inventory Action
                int windowSlot = buffer.readShort();
                readCreativeSlot(player, windowSlot, buffer);
                break;
            }
            case 0x1F: { // Player Block Placement
                int[] position = buffer.readPosition();
                int face = buffer.readVarInt();
                int hand = buffer.readVarInt();
                // Off-hand clicks arrive as a second packet for the same click; acting on
                // both would place the block twice.
                if (hand == 0) {
                    net.vibmc.world.BlockInteraction.useItem(
                            player, position[0], position[1], position[2], face);
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * Mirrors what a creative player drags into a slot into the server's own inventory.
     *
     * <p>The creative menu lives entirely on the client, so this packet is the only thing
     * that tells the server what the player is now holding - and without that, placing a
     * block would have nothing to place.
     */
    private static void readCreativeSlot(PlayerEntity player, int windowSlot, PacketBuffer buffer) {
        int slot = net.vibmc.player.PlayerManager.inventorySlotFor(windowSlot);
        if (slot < 0) {
            return;
        }
        int itemId = buffer.readShort();
        if (itemId < 0) {
            player.getInventory().setSlot(slot, new net.vibmc.item.ItemStack(
                    net.vibmc.item.ItemType.AIR, 0));
            return;
        }
        int count = buffer.readUnsignedByte();
        // The damage value and any NBT that follow describe details this server does not
        // model yet, so they are left unread - the packet buffer is discarded either way.
        net.vibmc.item.ItemType type = net.vibmc.item.ItemType.fromId(itemId);
        player.getInventory().setSlot(slot, new net.vibmc.item.ItemStack(type, count));
    }

    /**
     * Rejects NaN and infinite coordinates outright.
     *
     * <p>A malformed position is not a cheat to be measured, it is data that would poison
     * every later comparison - chunk maths, distance checks, the flight check - so the
     * connection is dropped rather than letting the value into the world state.
     */
    static boolean isFinitePosition(ClientConnection connection, double x, double y, double z) {
        if (isFinite(x) && isFinite(y) && isFinite(z)) {
            return true;
        }
        connection.disconnect("Invalid move player packet received");
        return false;
    }

    static boolean isFiniteLook(ClientConnection connection, float yaw, float pitch) {
        if (isFinite(yaw) && isFinite(pitch)) {
            return true;
        }
        connection.disconnect("Invalid move player packet received");
        return false;
    }

    /** Java 8 has no Double.isFinite on the primitive path we want, so spell it out. */
    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    @Override
    public void onDisconnect(ClientConnection connection, String reason) {
        VibMC.getInstance().getPlayerManager().removePlayer(player);
        player.getWorld().removeEntity(player);
    }
}
