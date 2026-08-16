package net.vibmc.network;

import net.vibmc.auth.GameProfile;
import net.vibmc.network.handler.PacketHandler;
import net.vibmc.server.VibMC;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
    private GameProfile profile;

    // Online-mode login state. AES/CFB8 is a stream cipher, so one Cipher per direction is
    // kept alive for the whole connection and fed incrementally as bytes arrive or leave.
    private byte[] verifyToken;
    private Cipher decryptCipher;
    private Cipher encryptCipher;

    private final Queue<byte[]> outgoing = new ConcurrentLinkedQueue<>();
    /** Guards "encrypt this frame, then queue it" so the two stay in step. */
    private final Object writeLock = new Object();
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

    /** The peer's IP, or null if the socket is already gone. Used for proxy trust checks. */
    public String remoteAddress() {
        try {
            java.net.SocketAddress remote = channel.getRemoteAddress();
            if (remote instanceof java.net.InetSocketAddress) {
                return ((java.net.InetSocketAddress) remote).getAddress().getHostAddress();
            }
        } catch (IOException e) {
            // socket already closed
        }
        return null;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public void setProfile(GameProfile profile) {
        this.profile = profile;
        if (profile != null && profile.name() != null) {
            this.username = profile.name();
        }
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(byte[] verifyToken) {
        this.verifyToken = verifyToken;
    }

    public boolean isEncrypted() {
        return encryptCipher != null;
    }

    /**
     * Switches both directions to AES/CFB8 using the shared secret as both key and IV,
     * exactly as the 1.12.2 protocol specifies.
     *
     * <p>Must be called after the Encryption Response has been read and before Login
     * Success is written - Login Success is the first encrypted packet in each direction.
     * Any bytes already buffered but not yet parsed belong to the encrypted stream, so
     * they are decrypted here rather than being parsed as plaintext.
     */
    public void enableEncryption(SecretKey secret) {
        try {
            IvParameterSpec iv = new IvParameterSpec(secret.getEncoded());
            Cipher in = Cipher.getInstance("AES/CFB8/NoPadding");
            in.init(Cipher.DECRYPT_MODE, secret, iv);
            Cipher out = Cipher.getInstance("AES/CFB8/NoPadding");
            out.init(Cipher.ENCRYPT_MODE, secret, iv);

            int pending = inLen - inOffset;
            if (pending > 0) {
                byte[] decrypted = in.update(inBuffer, inOffset, pending);
                System.arraycopy(decrypted, 0, inBuffer, inOffset, decrypted.length);
                inLen = inOffset + decrypted.length;
            }

            this.decryptCipher = in;
            this.encryptCipher = out;
        } catch (Exception e) {
            VibMC.getInstance().getLogger().warn(
                    "Could not enable encryption for %s: %s", describe(), e);
            disconnect("Encryption failed");
        }
    }

    private String describe() {
        return username != null ? username : "an unauthenticated client";
    }

    public void feed(byte[] data) {
        byte[] plain = data;
        if (decryptCipher != null) {
            plain = decryptCipher.update(data);
            if (plain == null || plain.length == 0) {
                return;
            }
        }
        ensureInCapacity(inLen + plain.length);
        System.arraycopy(plain, 0, inBuffer, inLen, plain.length);
        inLen += plain.length;
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

        // Encrypting and queueing have to happen together, under the same lock.
        //
        // Packets are sent from two threads - the tick loop streams chunks while the
        // network thread answers whatever the player just did - and AES/CFB8 is a stream
        // cipher, so a frame's ciphertext depends on every byte encrypted before it. If
        // two threads encrypt and then queue, the queue order can come out reversed, the
        // client decrypts the stream against the wrong keystream, and everything after
        // that point is noise: "Bad packet id", and the connection is gone.
        synchronized (writeLock) {
            outgoing.add(encryptOutbound(frame.toByteArray()));
        }
        try {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } catch (Exception e) {
            // key may already be invalidated
        }
    }

    /** Wraps plain text as a chat-component JSON string, escaping what would break it. */
    static String jsonText(String text) {
        StringBuilder out = new StringBuilder("{\"text\":\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append("\"}").toString();
    }

    /** CFB8 is a stream cipher, so frames can be encrypted one at a time as they are queued. */
    private byte[] encryptOutbound(byte[] frame) {
        if (encryptCipher == null) {
            return frame;
        }
        byte[] encrypted = encryptCipher.update(frame);
        return encrypted == null ? new byte[0] : encrypted;
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
            disconnect.writeString(jsonText(reason));
        } else if (protocolState == ProtocolState.LOGIN) {
            disconnect.writeVarInt(0x00);
            disconnect.writeString(jsonText(reason));
        } else {
            forceClose();
            return;
        }
        byte[] body = disconnect.toByteArray();
        PacketBuffer frame = new PacketBuffer();
        frame.writeVarInt(body.length);
        frame.writeBytes(body);
        try {
            // Flush anything already queued first, so the kick does not jump ahead of it
            // and (once encrypted) desynchronise the cipher stream. Encrypting the kick
            // itself takes the same lock as every other send, for the same reason.
            byte[] encrypted;
            synchronized (writeLock) {
                flushWrites();
                encrypted = encryptOutbound(frame.toByteArray());
            }
            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
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
