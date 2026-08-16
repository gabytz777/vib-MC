package net.vibmc.world;

import net.vibmc.world.gen.TerrainGenerator;
import net.vibmc.world.gen.structure.LavaPoolGenerator;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Surface lava pools are the overworld's obsidian supply, so they have to actually turn
 * up - and have to stay rare enough that finding one means something.
 */
class LavaPoolTest {
    private static final long SEED = 90210L;
    private static final int SWEEP = 18;  // 18 x 18 = 324 chunks

    private World overworld(Path dir) {
        String name = dir.resolve("pools").toString();
        return new World(SEED, name, Dimension.OVERWORLD, new WorldStorage(name));
    }

    @Test
    void poolsAppearRarelyAndSitInStone(@TempDir Path dir) {
        World world = overworld(dir);

        int chunksWithPools = 0;
        int lavaOverStone = 0;
        int lavaTotal = 0;

        for (int chunkX = 0; chunkX < SWEEP; chunkX++) {
            for (int chunkZ = 0; chunkZ < SWEEP; chunkZ++) {
                Chunk chunk = Chunk.generate(world, chunkX, chunkZ);
                boolean found = false;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = TerrainGenerator.SEA_LEVEL + 1; y < 120; y++) {
                            if (chunk.getBlock(x, y, z) != Block.LAVA.id()) {
                                continue;
                            }
                            found = true;
                            lavaTotal++;
                            if (chunk.getBlock(x, y - 1, z) == Block.STONE.id()) {
                                lavaOverStone++;
                            }
                        }
                    }
                }
                if (found) {
                    chunksWithPools++;
                }
            }
        }

        int chunks = SWEEP * SWEEP;
        assertTrue(chunksWithPools > 0, "some chunks in a 324-chunk sweep should have a pool");
        // Roughly one chunk in a hundred is considered, and flat dry land rejects some of
        // those, so anything past a few percent means the roll has drifted.
        assertTrue(chunksWithPools <= chunks / 25,
                "pools should be rare, found " + chunksWithPools + " in " + chunks + " chunks");
        assertEquals(lavaTotal, lavaOverStone, "every pool block rests on its stone bed");
    }

    @Test
    void theRollIsOneChunkInAHundred() {
        assertEquals(100, LavaPoolGenerator.CHUNKS_PER_POOL);
    }
}
