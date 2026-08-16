package net.vibmc.world;

import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Light has to flow. The old per-column sky light is what made trees render as black
 * lumps, so these tests are about light reaching places that are not directly under open
 * sky.
 */
class LightEngineTest {
    private World world(Path dir, Dimension dimension) {
        String name = dir.resolve("lit" + dimension.name()).toString();
        return new World(7L, name, dimension, new WorldStorage(name));
    }

    @Test
    void groundUnderACanopyIsShadedNotBlack(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        Chunk chunk = world.getChunk(0, 0);

        int surface = world.getHighestSolidY(8, 8);
        int canopy = surface + 5;
        for (int x = 6; x <= 10; x++) {
            for (int z = 6; z <= 10; z++) {
                for (int y = surface + 1; y < canopy; y++) {
                    chunk.setBlock(x, y, z, Block.AIR.id());
                }
                chunk.setBlock(x, canopy, z, Block.LEAVES.id());
            }
        }

        LightEngine.Light sky = LightEngine.skyLight(chunk);
        int underMiddle = sky.at(8, surface + 1, 8);

        assertTrue(underMiddle > 0,
                "the ground under a canopy should be shaded, not pitch black (was " + underMiddle + ")");
        assertEquals(15, sky.at(8, canopy + 1, 8), "open sky above the canopy is full brightness");
    }

    @Test
    void openSkyIsFullBrightAndSealedRockIsDark(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        Chunk chunk = world.getChunk(0, 0);

        LightEngine.Light sky = LightEngine.skyLight(chunk);
        assertEquals(15, sky.at(8, 200, 8), "nothing is above y=200 but sky");
        assertEquals(0, sky.at(8, 3, 8), "sunlight does not reach the bedrock layer");
    }

    @Test
    void glowstoneLightsItsSurroundings(@TempDir Path dir) {
        World world = world(dir, Dimension.NETHER);
        Chunk chunk = world.getChunk(0, 0);

        // Hollow out a pocket and hang a glowstone block in the middle of it.
        for (int x = 4; x <= 10; x++) {
            for (int z = 4; z <= 10; z++) {
                for (int y = 60; y <= 66; y++) {
                    chunk.setBlock(x, y, z, Block.AIR.id());
                }
            }
        }
        chunk.setBlock(7, 63, 7, Block.GLOWSTONE.id());

        LightEngine.Light light = LightEngine.blockLight(chunk);
        assertEquals(15, light.at(7, 63, 7), "the source itself is full brightness");
        assertEquals(14, light.at(8, 63, 7), "one block away is one level down");
        assertEquals(13, light.at(9, 63, 7), "and it keeps falling off with distance");
        // Eight blocks away it can be no brighter than eight levels down, however it got
        // there - the Nether's caverns are open, so there may well be a path.
        assertTrue(light.at(7, 63, 15) <= 7, "light never outruns its distance");
    }

    @Test
    void aPortalGlowsAndSectionsPackAsNibbles(@TempDir Path dir) {
        World world = world(dir, Dimension.OVERWORLD);
        Chunk chunk = world.getChunk(0, 0);

        chunk.setBlock(2, 100, 2, Block.AIR.id());
        chunk.setBlock(2, 100, 2, Block.NETHER_PORTAL.id());

        LightEngine.Light light = LightEngine.blockLight(chunk);
        assertEquals(11, light.at(2, 100, 2), "a portal gives off its own light");

        // Section 6 covers y=96..111; the level must survive the nibble packing intact.
        byte[] packed = light.section(6);
        assertEquals(2048, packed.length);
        int index = ((100 & 15) << 8) | (2 << 4) | 2;
        int nibble = (index & 1) == 0 ? packed[index >> 1] & 0x0F : (packed[index >> 1] >> 4) & 0x0F;
        assertEquals(11, nibble);
    }
}
