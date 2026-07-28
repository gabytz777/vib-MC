package net.vibmc;

import net.vibmc.network.PacketBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PacketBufferTest {

    @Test
    public void testWriteReadVarInt() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeVarInt(0);
        assertEquals(0, buf.readVarInt());

        buf.reset();
        buf.writeVarInt(1);
        assertEquals(1, buf.readVarInt());

        buf.reset();
        buf.writeVarInt(127);
        assertEquals(127, buf.readVarInt());

        buf.reset();
        buf.writeVarInt(128);
        assertEquals(128, buf.readVarInt());

        buf.reset();
        buf.writeVarInt(25565);
        assertEquals(25565, buf.readVarInt());

        buf.reset();
        buf.writeVarInt(2147483647);
        assertEquals(2147483647, buf.readVarInt());
    }

    @Test
    public void testWriteReadString() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeString("Hello, World!");
        assertEquals("Hello, World!", buf.readString());

        buf.reset();
        buf.writeString("");
        assertEquals("", buf.readString());

        buf.reset();
        String test = "A".repeat(100);
        buf.writeString(test);
        assertEquals(test, buf.readString());
    }

    @Test
    public void testWriteReadInt() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeInt(42);
        assertEquals(42, buf.readInt());

        buf.reset();
        buf.writeInt(-42);
        assertEquals(-42, buf.readInt());

        buf.reset();
        buf.writeInt(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, buf.readInt());

        buf.reset();
        buf.writeInt(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, buf.readInt());
    }

    @Test
    public void testWriteReadLong() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeLong(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, buf.readLong());

        buf.reset();
        buf.writeLong(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, buf.readLong());
    }

    @Test
    public void testWriteReadDouble() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeDouble(3.14159);
        assertEquals(3.14159, buf.readDouble(), 0.0001);

        buf.reset();
        buf.writeDouble(0.0);
        assertEquals(0.0, buf.readDouble(), 0.0);

        buf.reset();
        buf.writeDouble(-273.15);
        assertEquals(-273.15, buf.readDouble(), 0.0001);
    }

    @Test
    public void testWriteReadFloat() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeFloat(1.5f);
        assertEquals(1.5f, buf.readFloat(), 0.0001f);
    }

    @Test
    public void testWriteReadBoolean() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeBoolean(true);
        assertTrue(buf.readBoolean());

        buf.reset();
        buf.writeBoolean(false);
        assertFalse(buf.readBoolean());
    }

    @Test
    public void testWriteReadPosition() {
        PacketBuffer buf = new PacketBuffer();
        buf.writePosition(10, 20, 30);

        buf.reset();
        long val = buf.readLong();
        int x = (int) (val >> 38);
        int y = (int) ((val >> 26) & 0xFFF);
        int z = (int) (val << 38 >> 38);
        assertEquals(10, x);
        assertEquals(20, y);
        assertEquals(30, z);
    }

    @Test
    public void testWriteReadByteArray() {
        PacketBuffer buf = new PacketBuffer();
        byte[] data = {1, 2, 3, 4, 5};
        buf.writeByteArray(data);

        byte[] result = buf.readByteArray();
        assertArrayEquals(data, result);
    }

    @Test
    public void testBufferGrowth() {
        PacketBuffer buf = new PacketBuffer(16);
        for (int i = 0; i < 1000; i++) {
            buf.writeInt(i);
        }

        for (int i = 0; i < 1000; i++) {
            assertEquals(i, buf.readInt());
        }
    }

    @Test
    public void testVarLong() {
        PacketBuffer buf = new PacketBuffer();
        buf.writeVarLong(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, buf.readVarLong());

        buf.reset();
        buf.writeVarLong(0L);
        assertEquals(0L, buf.readVarLong());
    }
}
