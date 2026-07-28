package net.vibmc.network;

public interface Packet {
    int getPacketId();
    void read(PacketBuffer buffer);
    void write(PacketBuffer buffer);
}
