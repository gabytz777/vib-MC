package net.vibmc.network;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PacketBuffer {
    private byte[] data;
    private int readIndex;
    private int writeIndex;

    public PacketBuffer() {
        this(64);
    }

    public PacketBuffer(int initialCapacity) {
        this.data = new byte[Math.max(initialCapacity, 1)];
    }

    public PacketBuffer(byte[] data) {
        this.data = data;
        this.writeIndex = data.length;
    }

    public int readableBytes() {
        return writeIndex - readIndex;
    }

    // ---- read ----

    public byte readByte() {
        return data[readIndex++];
    }

    public int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public short readShort() {
        int value = (readUnsignedByte() << 8) | readUnsignedByte();
        return (short) value;
    }

    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    public int readInt() {
        return (readUnsignedByte() << 24) | (readUnsignedByte() << 16)
                | (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public long readLong() {
        long high = readInt();
        long low = readInt() & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public int readVarInt() {
        int value = 0;
        int shift = 0;
        while (true) {
            if (readIndex >= writeIndex) {
                throw new IndexOutOfBoundsException("VarInt truncated");
            }
            byte b = readByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
            if (shift >= 32) {
                throw new RuntimeException("VarInt too big");
            }
        }
    }

    public String readString() {
        int length = readVarInt();
        byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(data, readIndex, bytes, 0, length);
        readIndex += length;
        return bytes;
    }

    // ---- write ----

    public void writeByte(int value) {
        ensureCapacity(writeIndex + 1);
        data[writeIndex++] = (byte) value;
    }

    public void writeBoolean(boolean value) {
        writeByte(value ? 1 : 0);
    }

    public void writeShort(int value) {
        writeByte((value >> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    public void writeInt(int value) {
        writeByte((value >> 24) & 0xFF);
        writeByte((value >> 16) & 0xFF);
        writeByte((value >> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    public void writeLong(long value) {
        writeInt((int) (value >> 32));
        writeInt((int) value);
    }

    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    public void writeVarInt(int value) {
        do {
            int part = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            writeByte(part);
        } while (value != 0);
    }

    public void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        writeBytes(bytes);
    }

    public void writeBytes(byte[] bytes) {
        ensureCapacity(writeIndex + bytes.length);
        System.arraycopy(bytes, 0, data, writeIndex, bytes.length);
        writeIndex += bytes.length;
    }

    public void writePosition(int x, int y, int z) {
        // 1.12.2 format: X = high 26 bits, Y = middle 12 bits, Z = low 26 bits
        long value = (((long) x & 0x3FFFFFFL) << 38)
                | (((long) y & 0xFFFL) << 26)
                | ((long) z & 0x3FFFFFFL);
        writeLong(value);
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(data, writeIndex);
    }

    private void ensureCapacity(int needed) {
        if (needed <= data.length) {
            return;
        }
        int newCapacity = Math.max(data.length << 1, needed);
        data = Arrays.copyOf(data, newCapacity);
    }
}
