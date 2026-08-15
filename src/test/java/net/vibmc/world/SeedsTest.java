package net.vibmc.world;

import net.vibmc.world.storage.LevelData;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedsTest {
    @Test
    void blankSettingsAreRecognised() {
        assertTrue(Seeds.isBlank(null));
        assertTrue(Seeds.isBlank(""));
        assertTrue(Seeds.isBlank("   "));
        assertFalse(Seeds.isBlank("0"));
    }

    @Test
    void numericSeedsAreUsedAsIs() {
        assertEquals(0L, Seeds.resolve("0"));
        assertEquals(12345L, Seeds.resolve("12345"));
        assertEquals(-987L, Seeds.resolve("-987"));
        assertEquals(1227398065945640716L, Seeds.resolve("  1227398065945640716  "));
    }

    @Test
    void textSeedsHashDeterministically() {
        long first = Seeds.resolve("vibmc");
        long second = Seeds.resolve("vibmc");

        assertEquals(first, second, "the same text seed must always produce the same world");
        assertNotEquals(Seeds.resolve("vibmc"), Seeds.resolve("vibmd"));
    }

    @Test
    void textSeedsSpreadAcrossTheLongRange() {
        // A plain String.hashCode() would leave every text seed in the small-integer
        // range; the avalanche mix should push them well beyond 32 bits.
        boolean anyLarge = false;
        for (String text : new String[]{"a", "hello", "vib-MC", "a slightly longer seed"}) {
            if (Math.abs(Seeds.resolve(text)) > (1L << 40)) {
                anyLarge = true;
            }
        }
        assertTrue(anyLarge, "text seeds should use the whole 64-bit range");
    }

    @Test
    void blankSettingRollsARandomSeed() {
        long first = Seeds.resolve("");
        long second = Seeds.resolve("");

        assertNotEquals(0L, first, "a blank seed must not silently mean 0");
        assertNotEquals(first, second, "each new world should get its own random seed");
    }

    @Test
    void randomSeedIsPersistedAndRestored(@TempDir Path dir) throws IOException {
        String name = dir.resolve("world").toString();

        // First boot with a blank seed: a random one is rolled and written to level.dat.
        long rolled = Seeds.resolve("");
        WorldStorage storage = new WorldStorage(name);
        storage.writeLevel(new LevelData(rolled, 0L, 0L, "clear"));

        // Restart: the saved seed is read back unchanged.
        assertEquals(rolled, new WorldStorage(name).readLevel().seed());
    }
}
