package net.vibmc.network;

import net.vibmc.network.handler.PacketHandler;
import net.vibmc.server.VibMC;
import net.vibmc.server.util.UUIDUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection {
    private final SocketChannel channel;
    private final SelectionKey key;
    private ProtocolState state;
    private PacketHandler handler;
    private String username;
    private UUID uuid;
    private final ConcurrentLinkedQueue<byte[]> sendQueue;

    public ClientConnection(SocketChannel channel, SelectionKey key) {
        this.channel = channel;
        this.key = key;
        this.state = ProtocolState.HANDSHAKE;
        this.sendQueue = new ConcurrentLinkedQueue<>();
    }

    public void setHandler(PacketHandler handler) {
        this.handler = handler;
    }

    public PacketHandler getHandler() {
        return handler;
    }

    public void setState(ProtocolState state) {
        this.state = state;
    }

    public ProtocolState getState() {
        return state;
    }

    public void setUsername(String username) {
        this.username = username;
        this.uuid = UUIDUtil.fromOfflinePlayer(username);
    }

    public String getUsername() { return username; }
    public UUID getUuid() { return uuid; }

    public void sendPacket(Packet packet) {
        try {
            PacketBuffer buffer = new PacketBuffer();
            buffer.writeVarInt(packet.getPacketId());
            packet.write(buffer);

            byte[] packetData = buffer.toByteArray();

            PacketBuffer frameBuffer = new PacketBuffer();
            frameBuffer.writeVarInt(packetData.length);
            frameBuffer.writeBytes(packetData);

            sendQueue.add(frameBuffer.toByteArray());
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        } catch (Exception e) {
            VibMC.getInstance().getLogger().severe("Error sending packet to %s: %s", getUsername(), e);
        }
    }

    public byte[] dequeuePacket() {
        return sendQueue.poll();
    }

    public boolean hasQueuedPackets() {
        return !sendQueue.isEmpty();
    }

    public void disconnect(String reason) {
        if (state == ProtocolState.DISCONNECTED) return;
        state = ProtocolState.DISCONNECTED;
        if (handler != null) {
            handler.onDisconnect(this, reason);
        }
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public SocketAddress getRemoteAddress() {
        try {
            return channel.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }

    public SocketChannel getChannel() { return channel; }
    public SelectionKey getKey() { return key; }
}
