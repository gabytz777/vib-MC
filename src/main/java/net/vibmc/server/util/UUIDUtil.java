package net.vibmc.server.util;

import java.util.UUID;

public class UUIDUtil {
    public static UUID fromOfflinePlayer(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buf = new byte[16];
        for (int i = 0; i < 8; i++) {
            buf[i] = (byte) (msb >> (8 * (7 - i)));
            buf[8 + i] = (byte) (lsb >> (8 * (7 - i)));
        }
        return buf;
    }

    public static UUID fromBytes(byte[] bytes) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }
}
