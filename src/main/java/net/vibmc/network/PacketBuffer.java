package net.vibmc.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketBuffer {
    private byte[] data;
    private int writePos;
    private int readPos;

    public PacketBuffer() {
        this(128);
    }

    public PacketBuffer(int capacity) {
        this.data = new byte[capacity];
        this.writePos = 0;
        this.readPos = 0;
    }

    public PacketBuffer(byte[] data) {
        this.data = data;
        this.writePos = data.length;
        this.readPos = 0;
    }

    public void writeByte(int b) {
        ensureCapacity(1);
        data[writePos++] = (byte) b;
    }

    public void writeShort(int s) {
        ensureCapacity(2);
        data[writePos++] = (byte) ((s >> 8) & 0xFF);
        data[writePos++] = (byte) (s & 0xFF);
    }

    public void writeInt(int i) {
        ensureCapacity(4);
        data[writePos++] = (byte) ((i >> 24) & 0xFF);
        data[writePos++] = (byte) ((i >> 16) & 0xFF);
        data[writePos++] = (byte) ((i >> 8) & 0xFF);
        data[writePos++] = (byte) (i & 0xFF);
    }

    public void writeLong(long l) {
        ensureCapacity(8);
        for (int i = 7; i >= 0; i--) {
            data[writePos++] = (byte) ((l >> (i * 8)) & 0xFF);
        }
    }

    public void writeFloat(float f) {
        writeInt(Float.floatToRawIntBits(f));
    }

    public void writeDouble(double d) {
        writeLong(Double.doubleToRawLongBits(d));
    }

    public void writeBoolean(boolean b) {
        writeByte(b ? 1 : 0);
    }

    public void writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, data, writePos, bytes.length);
        writePos += bytes.length;
    }

    public void writeUUID(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public void writeVarInt(int value) {
        ensureCapacity(5);
        do {
            byte temp = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) {
                temp |= 0x80;
            }
            data[writePos++] = temp;
        } while (value != 0);
    }

    public void writeVarLong(long value) {
        ensureCapacity(10);
        do {
            byte temp = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) {
                temp |= 0x80;
            }
            data[writePos++] = temp;
        } while (value != 0);
    }

    public void writeBytes(byte[] bytes) {
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, data, writePos, bytes.length);
        writePos += bytes.length;
    }

    public void writeByteArray(byte[] bytes) {
        writeVarInt(bytes.length);
        writeBytes(bytes);
    }

    public void writePosition(int x, int y, int z) {
        writeLong(((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF));
    }

    public int readByte() {
        return data[readPos++] & 0xFF;
    }

    public int readShort() {
        int val = (data[readPos] & 0xFF) << 8 | (data[readPos + 1] & 0xFF);
        readPos += 2;
        return val;
    }

    public int readInt() {
        int val = (data[readPos] & 0xFF) << 24 | (data[readPos + 1] & 0xFF) << 16
                | (data[readPos + 2] & 0xFF) << 8 | (data[readPos + 3] & 0xFF);
        readPos += 4;
        return val;
    }

    public long readLong() {
        long val = 0;
        for (int i = 0; i < 8; i++) {
            val = (val << 8) | (data[readPos + i] & 0xFF);
        }
        readPos += 8;
        return val;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public String readString() {
        int length = readVarInt();
        String s = new String(data, readPos, length, StandardCharsets.UTF_8);
        readPos += length;
        return s;
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    public int readVarInt() {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            b = data[readPos++];
            value |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 0x80) != 0);
        return value;
    }

    public long readVarLong() {
        long value = 0;
        int shift = 0;
        byte b;
        do {
            b = data[readPos++];
            value |= (long) (b & 0x7F) << shift;
            shift += 7;
            if (shift > 70) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((b & 0x80) != 0);
        return value;
    }

    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(data, readPos, bytes, 0, length);
        readPos += length;
        return bytes;
    }

    public byte[] readByteArray() {
        int len = readVarInt();
        return readBytes(len);
    }

    public int readableBytes() {
        return writePos - readPos;
    }

    public byte[] toByteArray() {
        byte[] result = new byte[writePos];
        System.arraycopy(data, 0, result, 0, writePos);
        return result;
    }

    public int getWritePos() { return writePos; }
    public int getReadPos() { return readPos; }

    private void ensureCapacity(int needed) {
        if (writePos + needed >= data.length) {
            byte[] newData = new byte[Math.max(data.length * 2, writePos + needed)];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
    }

    public void reset() {
        readPos = 0;
        writePos = 0;
    }
}
