package net.vibmc.network;

import net.vibmc.network.handler.HandshakeHandler;
import net.vibmc.util.Json;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolTest {
    @Test
    void outdatedClientsAndServersGetTheRightMessage() {
        // 340 is 1.12.2. A lower number means the player is behind, a higher one means we are.
        assertEquals("Outdated client! Please use Minecraft 1.12.2.",
                HandshakeHandler.versionKickMessage(47));
        assertEquals("Outdated server! This server supports Minecraft 1.12.2 only.",
                HandshakeHandler.versionKickMessage(760));
        assertEquals(340, HandshakeHandler.SUPPORTED_PROTOCOL);
    }

    @Test
    void kickReasonsAreEscapedIntoValidJson() {
        // Kick text reaches the client as a chat component. An unescaped quote would make
        // the packet unparseable, which shows up as a confusing decode error rather than
        // the message the operator wrote.
        String json = ClientConnection.jsonText("Server closed");
        assertEquals("{\"text\":\"Server closed\"}", json);

        Map<String, Object> parsed = Json.parseObject(
                ClientConnection.jsonText("say \"hi\"\nand \\ that"));
        assertEquals("say \"hi\"\nand \\ that", Json.string(parsed, "text"),
                "a reason with quotes, newlines and backslashes must survive the round trip");
    }

    @Test
    void varIntsRoundTripThroughThePacketBuffer() {
        int[] values = {0, 1, 127, 128, 255, 2097151, Integer.MAX_VALUE, -1};
        for (int value : values) {
            PacketBuffer out = new PacketBuffer();
            out.writeVarInt(value);
            assertEquals(value, new PacketBuffer(out.toByteArray()).readVarInt(),
                    "varint round trip for " + value);
        }
    }

    @Test
    void stringsRoundTripIncludingColourCodes() {
        PacketBuffer out = new PacketBuffer();
        out.writeString("§e_poisoned joined the game");

        PacketBuffer in = new PacketBuffer(out.toByteArray());
        // The section sign is two bytes in UTF-8; the length prefix counts bytes, not
        // characters, and getting that wrong is what breaks the client's string decoder.
        assertEquals("§e_poisoned joined the game", in.readString());
    }

    @Test
    void positionsPackIntoTheVanillaBitLayout() {
        PacketBuffer out = new PacketBuffer();
        out.writePosition(100, 64, -200);

        long packed = new PacketBuffer(out.toByteArray()).readLong();
        assertEquals(100, (int) (packed >> 38));
        assertEquals(64, (int) ((packed >> 26) & 0xFFF));
        // Z is stored in the low 26 bits as a signed value.
        int z = (int) (packed & 0x3FFFFFF);
        if (z >= (1 << 25)) {
            z -= (1 << 26);
        }
        assertEquals(-200, z);
    }

    @Test
    void jsonParserHandlesTheShapesMojangSends() {
        Map<String, Object> root = Json.parseObject(
                "{\"id\":\"abc\",\"n\":12.5,\"ok\":true,\"none\":null,"
                        + "\"list\":[{\"k\":\"v\"},\"plain\"]}");

        assertEquals("abc", Json.string(root, "id"));
        assertEquals(12.5, (Double) root.get("n"), 0.0001);
        assertEquals(Boolean.TRUE, root.get("ok"));
        assertTrue(root.containsKey("none"));
        assertEquals(2, ((java.util.List<?>) root.get("list")).size());
    }

    @Test
    void jsonParserRejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\" 1}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[1,]"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} trailing"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse(null));
    }

    @Test
    void jsonParserDecodesEscapesAndUnicode() {
        Map<String, Object> root = Json.parseObject(
                "{\"s\":\"line\\nbreak \\u00a76 \\\"quoted\\\"\"}");
        assertEquals("line\nbreak §6 \"quoted\"", Json.string(root, "s"));
    }
}
