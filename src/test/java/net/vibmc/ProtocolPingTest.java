package net.vibmc;

import net.vibmc.server.VibMC;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;

public class ProtocolPingTest {

    @Test
    public void testServerStatusPing() throws Exception {
        // This test validates the Minecraft protocol responds correctly to pings
        // Start server in a thread
        Thread serverThread = new Thread(() -> {
            VibMC server = new VibMC(new String[]{});
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(2000);

        try (Socket socket = new Socket("127.0.0.1", 25565)) {
            socket.setSoTimeout(5000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Handshake packet (id=0x00, next_state=1 for status)
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshakeOut = new DataOutputStream(handshakeBytes);
            writeVarInt(handshakeOut, 0x00); // packet id
            writeVarInt(handshakeOut, 340);  // protocol version
            writeVarInt(handshakeOut, 9);    // "localhost" length
            handshakeOut.writeBytes("localhost");
            handshakeOut.writeShort(25565);   // port
            writeVarInt(handshakeOut, 1);    // next state: status

            byte[] handshakeData = handshakeBytes.toByteArray();
            writeVarInt(out, handshakeData.length);
            out.write(handshakeData);

            // Status request (0x00)
            writeVarInt(out, 1); // length of packet
            writeVarInt(out, 0x00); // packet id
            out.flush();

            // Read response
            int packetLength = readVarInt(in);
            assertTrue(packetLength > 0, "Response should have data");

            int packetId = readVarInt(in);
            assertEquals(0x00, packetId, "Status response should be packet id 0x00");

            String json = readVarIntString(in);
            assertNotNull(json);
            assertTrue(json.contains("vib-MC"), "Response should contain vib-MC");
            assertTrue(json.contains("340"), "Response should contain protocol 340");

            System.out.println("Ping response: " + json);

            // Ping packet (0x01)
            long testPayload = 12345L;
            ByteArrayOutputStream pingBytes = new ByteArrayOutputStream();
            DataOutputStream pingOut = new DataOutputStream(pingBytes);
            writeVarInt(pingOut, 0x01);
            pingOut.writeLong(testPayload);
            byte[] pingData = pingBytes.toByteArray();
            writeVarInt(out, pingData.length);
            out.write(pingData);
            out.flush();

            // Read pong response
            int pongLength = readVarInt(in);
            assertTrue(pongLength > 0);
            int pongId = readVarInt(in);
            assertEquals(0x01, pongId);
            long pongPayload = in.readLong();
            assertEquals(testPayload, pongPayload);

            System.out.println("Ping-pong successful!");
        }
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        do {
            byte temp = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) temp |= 0x80;
            out.writeByte(temp);
        } while (value != 0);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            b = in.readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt too big");
        } while ((b & 0x80) != 0);
        return value;
    }

    private String readVarIntString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
