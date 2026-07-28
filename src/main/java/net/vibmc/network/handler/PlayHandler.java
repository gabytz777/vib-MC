package net.vibmc.network.handler;

import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.server.VibMC;

public class PlayHandler implements PacketHandler {
    @Override
    public void handle(ClientConnection connection, int packetId, PacketBuffer buffer) {
        var server = VibMC.getInstance();
        var player = server.getPlayerManager().getPlayer(connection);
        if (player == null) return;

        switch (packetId) {
            case 0x0B -> handleKeepAlive(connection, buffer);
            case 0x0D -> handlePlayerPosition(connection, buffer, player);
            case 0x0E -> handlePlayerPositionRotation(connection, buffer, player);
            case 0x0F -> handlePlayerRotation(connection, buffer, player);
            case 0x0C -> handlePlayerMovement(connection, buffer, player);
            case 0x02 -> handleChatMessage(connection, buffer);
            case 0x03 -> handleClientCommand(connection, buffer, player);
            case 0x14 -> handlePlayerAction(connection, buffer, player);
            case 0x07 -> handleClickContainer(connection, buffer, player);
            case 0x08 -> handleCloseContainer(connection, buffer, player);
            case 0x1B -> handleSetCreativeSlot(connection, buffer, player);
            case 0x1A -> handleHeldItemChange(connection, buffer, player);
            default -> {
                // Unknown packet - ignore in development
            }
        }
    }

    private void handleKeepAlive(ClientConnection connection, PacketBuffer buffer) {
        long id = buffer.readLong();
        var server = VibMC.getInstance();
        var player = server.getPlayerManager().getPlayer(connection);
        if (player != null) {
            player.handleKeepAlive(id);
        }
    }

    private void handlePlayerPosition(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        boolean onGround = buffer.readBoolean();
        player.setPosition(x, y, z);
        player.setOnGround(onGround);
        sendPositionAck(connection);
    }

    private void handlePlayerPositionRotation(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        float yaw = buffer.readFloat();
        float pitch = buffer.readFloat();
        boolean onGround = buffer.readBoolean();
        player.setPositionAndRotation(x, y, z, yaw, pitch);
        player.setOnGround(onGround);
        sendPositionAck(connection);
    }

    private void handlePlayerRotation(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        float yaw = buffer.readFloat();
        float pitch = buffer.readFloat();
        boolean onGround = buffer.readBoolean();
        player.setRotation(yaw, pitch);
        player.setOnGround(onGround);
    }

    private void handlePlayerMovement(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        boolean onGround = buffer.readBoolean();
        player.setOnGround(onGround);
    }

    private void handleChatMessage(ClientConnection connection, PacketBuffer buffer) {
        String message = buffer.readString();
        var server = VibMC.getInstance();
        var player = server.getPlayerManager().getPlayer(connection);
        if (player != null) {
            server.getPlayerManager().handleChat(player, message);
        }
    }

    private void handleClientCommand(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        int actionId = buffer.readVarInt();
        switch (actionId) {
            case 0 -> player.performRespawn();
            case 1 -> player.setSneaking(true);
            case 2 -> player.setSneaking(false);
            case 3 -> player.setSprinting(true);
            case 4 -> player.setSprinting(false);
        }
    }

    private void handlePlayerAction(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        int actionId = buffer.readVarInt();
        if (actionId == 2) {
            long position = buffer.readLong();
            int face = buffer.readByte();
            player.handleBlockBreak(position, face);
        } else if (actionId == 0) {
            buffer.readLong(); // position
            buffer.readByte(); // face
        }
    }

    private void handleClickContainer(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        buffer.readByte(); // window id
        buffer.readShort(); // slot
        buffer.readByte(); // button
        buffer.readShort(); // action number
        buffer.readVarInt(); // mode
    }

    private void handleCloseContainer(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        buffer.readByte(); // window id
    }

    private void handleSetCreativeSlot(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        buffer.readShort();
    }

    private void handleHeldItemChange(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        int slot = buffer.readShort();
        player.setHeldItemSlot(slot);
    }

    private void handleSetPlayerInventory(ClientConnection connection, PacketBuffer buffer, net.vibmc.entity.PlayerEntity player) {
        // Inventory set action
    }

    private void sendPositionAck(ClientConnection connection) {
        connection.sendPacket(new net.vibmc.network.Packet() {
            public int getPacketId() { return 0x2F; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeDouble(0);
                b.writeDouble(64);
                b.writeDouble(0);
                b.writeFloat(0);
                b.writeFloat(0);
                b.writeByte(0);
                b.writeVarInt(0);
            }
        });
    }

    @Override
    public void onDisconnect(ClientConnection connection, String reason) {
        var server = VibMC.getInstance();
        var player = server.getPlayerManager().getPlayer(connection);
        if (player != null) {
            server.getPlayerManager().removePlayer(player);
        }
    }

    @Override
    public void onConnect(ClientConnection connection) {
    }
}
