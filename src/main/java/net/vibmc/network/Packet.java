package net.vibmc.network;

public abstract class Packet {
    public abstract int getPacketId();

    public abstract void read(PacketBuffer buffer);

    public abstract void write(PacketBuffer buffer);
}
