package net.vibmc.network;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Packets leave from two threads at once - the tick loop streaming chunks and the network
 * thread answering what the player just did - and AES/CFB8 is a stream cipher, so a frame
 * decrypts correctly only if it reaches the client in the order it was encrypted.
 *
 * <p>When encrypting and queueing were separate steps, two threads could encrypt A then B
 * and queue B then A. Everything after that point decrypted to noise, and the client died
 * with "Bad packet id". This drives the same traffic through a connection and checks the
 * stream still reads back cleanly.
 */
class EncryptedSendOrderTest {
    private static final int THREADS = 4;
    private static final int PACKETS_EACH = 250;

    /** A packet with a recognisable body, so a garbled stream cannot pass unnoticed. */
    private static Packet numbered(int id, int value) {
        return new Packet() {
            public int getPacketId() {
                return id;
            }

            public void read(PacketBuffer b) {
            }

            public void write(PacketBuffer b) {
                b.writeInt(value);
            }
        };
    }

    @Test
    void concurrentSendsStayInStepWithTheCipher() throws Exception {
        SecretKey secret = new SecretKeySpec(new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");

        // No socket and no selection key: sending only queues bytes, and the interest-op
        // nudge that follows is already written to tolerate a key that has gone away.
        ClientConnection connection = new ClientConnection(null, null);
        connection.enableEncryption(secret);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            int threadId = t;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < PACKETS_EACH; i++) {
                        connection.sendPacket(numbered(0x20 + threadId, i));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "senders should finish promptly");

        // Everything queued, in queue order, is the byte stream the client would receive.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] chunk;
        while ((chunk = connection.dequeuePacket()) != null) {
            stream.write(chunk);
        }

        Cipher decrypt = Cipher.getInstance("AES/CFB8/NoPadding");
        decrypt.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(secret.getEncoded()));
        PacketBuffer plain = new PacketBuffer(decrypt.update(stream.toByteArray()));

        List<Integer> ids = new ArrayList<>();
        while (plain.readableBytes() > 0) {
            int length = plain.readVarInt();
            int before = plain.readableBytes();
            int id = plain.readVarInt();
            int value = plain.readInt();

            assertTrue(id >= 0x20 && id < 0x20 + THREADS, "decoded a bogus packet id: " + id);
            assertTrue(value >= 0 && value < PACKETS_EACH, "decoded a bogus body: " + value);
            assertEquals(length, before - plain.readableBytes(), "frame length must match its body");
            ids.add(id);
        }

        assertEquals(THREADS * PACKETS_EACH, ids.size(), "every packet should arrive intact");
    }
}
