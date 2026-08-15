package net.vibmc.world;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Turns the {@code seed} entry in server.properties into the long the generator uses.
 *
 * <p>A blank setting means "no seed chosen": a brand-new world rolls a random one, so a
 * fresh install does not generate the same terrain for everybody. Numeric values are used
 * as-is, and anything else is hashed deterministically, so a text seed like {@code vibmc}
 * always produces the same world.
 *
 * <p>The resolved seed is recorded in the world's {@code level.dat}, never written back to
 * server.properties: the saved world is the authority on the seed it was generated with.
 */
public final class Seeds {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Seeds() {
    }

    /** True when the configured seed is blank, meaning a new world should roll a random one. */
    public static boolean isBlank(String setting) {
        return setting == null || setting.trim().isEmpty();
    }

    /**
     * Resolves a configured seed setting to a generator seed.
     *
     * @param setting the raw {@code seed} value from server.properties
     * @return the numeric value for numeric input, a stable hash for text input, or a fresh
     *         random seed when the setting is blank
     */
    public static long resolve(String setting) {
        if (isBlank(setting)) {
            return random();
        }
        String trimmed = setting.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return hash(trimmed);
        }
    }

    /** A fresh random 64-bit seed. */
    public static long random() {
        return RANDOM.nextLong();
    }

    /**
     * Deterministic 64-bit hash of a text seed. Uses the same avalanche mix as the terrain
     * generator so text seeds spread across the whole long range instead of clustering in
     * the small-integer space that String.hashCode() would produce.
     */
    public static long hash(String text) {
        long h = 0xCBF29CE484222325L; // FNV-1a offset basis
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xFF);
            h *= 0x100000001B3L; // FNV-1a prime
        }
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }
}
