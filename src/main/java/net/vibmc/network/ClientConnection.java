package net.vibmc.network;

import net.vibmc.network.handler.PacketHandler;
import net.vibmc.server.VibMC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection {
    private static final int MAX_IN_BUFFER = 1 << 20;

    private final SocketChannel channel;
    private final SelectionKey key;
    private PacketHandler handler;
    private ProtocolState protocolState = ProtocolState.HANDSHAKE;
    private String username;

    private final Queue<byte[]> outgoing = new ConcurrentLinkedQueue<>();
    private ByteBuffer currentOut;

    private byte[] inBuffer = new byte[8192];
    private int inLen;
    private int inOffset;

    public ClientConnection(SocketChannel channel, SelectionKey key) {
        this.channel = channel;
        this.key = key;
    }

    public SocketChannel channel() {
        return channel;
    }

    public SelectionKey key() {
        return key;
    }

    public void setHandler(PacketHandler handler) {
        this.handler = handler;
    }

    public PacketHandler getHandler() {
        return handler;
    }

    public ProtocolState protocolState() {
        return protocolState;
    }

    public void setProtocolState(ProtocolState protocolState) {
        this.protocolState = protocolState;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void feed(byte[] data) {
        ensureInCapacity(inLen + data.length);
        System.arraycopy(data, 0, inBuffer, inLen, data.length);
        inLen += data.length;
        if (inLen > MAX_IN_BUFFER) {
            forceClose();
            return;
        }
        process();
    }

    private void process() {
        while (true) {
            int start = inOffset;
            Integer packetLength = tryReadVarInt();
            if (packetLength == null) {
                inOffset = start;
                break;
            }
            int afterLength = inOffset;
            if (packetLength < 0 || afterLength + packetLength > inLen) {
                inOffset = start;
                break;
            }
            Integer packetId = tryReadVarInt();
            if (packetId == null) {
                inOffset = start;
                break;
            }
            int idSize = varIntSize(packetId);
            int payloadLength = packetLength - idSize;
            if (payloadLength < 0 || afterLength + idSize + payloadLength > inLen) {
                inOffset = start;
                break;
            }
            byte[] payload = Arrays.copyOfRange(inBuffer, afterLength + idSize, afterLength + idSize + payloadLength);
            inOffset = afterLength + idSize + payloadLength;
            if (handler != null) {
                try {
                    VibMC.getInstance().getLogger().debug("Handling packet 0x%02X (%d bytes payload)", packetId, payloadLength);
                    handler.handle(this, packetId, new PacketBuffer(payload));
                } catch (Exception e) {
                    VibMC.getInstance().getLogger().warn("Error handling packet 0x%02X from %s: %s",
                            packetId, getUsername() != null ? getUsername() : "unknown", e);
                }
            }
        }
        if (inOffset > 0) {
            int remaining = inLen - inOffset;
            System.arraycopy(inBuffer, inOffset, inBuffer, 0, remaining);
            inLen = remaining;
            inOffset = 0;
        }
    }

    private Integer tryReadVarInt() {
        int value = 0;
        int shift = 0;
        while (inOffset < inLen) {
            byte b = inBuffer[inOffset++];
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
            if (shift >= 32) {
                return null;
            }
        }
        return null;
    }

    private static int varIntSize(int value) {
        int size = 0;
        do {
            size++;
            value >>>= 7;
        } while (value != 0);
        return size;
    }

    public void sendPacket(Packet packet) {
        PacketBuffer payload = new PacketBuffer();
        payload.writeVarInt(packet.getPacketId());
        packet.write(payload);
        byte[] body = payload.toByteArray();
        PacketBuffer frame = new PacketBuffer();
        frame.writeVarInt(body.length);
        frame.writeBytes(body);
        outgoing.add(frame.toByteArray());
        try {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } catch (Exception e) {
            // key may already be invalidated
        }
    }

    public boolean hasQueuedPackets() {
        return currentOut != null || !outgoing.isEmpty();
    }

    public byte[] dequeuePacket() {
        return outgoing.poll();
    }

    /**
     * Writes queued packets to the socket, honouring partial non-blocking writes.
     * Returns false if the socket buffer is full and the current packet is unfinished.
     */
    public boolean flushWrites() throws IOException {
        while (true) {
            if (currentOut == null) {
                byte[] next = outgoing.poll();
                if (next == null) {
                    return true;
                }
                currentOut = ByteBuffer.wrap(next);
            }
            channel.write(currentOut);
            if (currentOut.hasRemaining()) {
                return false;
            }
            currentOut = null;
        }
    }

    public void disconnect(String reason) {
        if (!channel.isOpen() || !key.isValid()) {
            forceClose();
            return;
        }
        PacketBuffer disconnect = new PacketBuffer();
        if (protocolState == ProtocolState.PLAY) {
            disconnect.writeVarInt(0x1A);
            disconnect.writeString("{\"text\":\"" + reason + "\"}");
        } else if (protocolState == ProtocolState.LOGIN) {
            disconnect.writeVarInt(0x00);
            disconnect.writeString("{\"text\":\"" + reason + "\"}");
        } else {
            forceClose();
            return;
        }
        byte[] body = disconnect.toByteArray();
        PacketBuffer frame = new PacketBuffer();
        frame.writeVarInt(body.length);
        frame.writeBytes(body);
        try {
            ByteBuffer buffer = ByteBuffer.wrap(frame.toByteArray());
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            // connection already gone
        }
        forceClose();
    }

    public void forceClose() {
        try {
            key.cancel();
        } catch (Exception ignored) {
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    private void ensureInCapacity(int needed) {
        if (needed <= inBuffer.length) {
            return;
        }
        int newCapacity = Math.max(inBuffer.length << 1, needed);
        inBuffer = Arrays.copyOf(inBuffer, newCapacity);
    }
}
