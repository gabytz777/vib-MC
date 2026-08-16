package net.vibmc.world;

import net.vibmc.world.gen.NetherGenerator;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Nether has to be a place, not a solid slab with a hole cut in it for the portal.
 */
class NetherGenerationTest {
    private static final long SEED = 31337L;

    private World nether(Path dir) {
        String name = dir.resolve("hell").toString();
        return new World(SEED, name, Dimension.NETHER, new WorldStorage(name));
    }

    @Test
    void thereIsSomewhereToStandUnderAnOpenCavern(@TempDir Path dir) {
        World world = nether(dir);

        int columns = 0;
        int walkable = 0;
        int openBlocks = 0;

        for (int chunkX = 0; chunkX < 3; chunkX++) {
            for (int chunkZ = 0; chunkZ < 3; chunkZ++) {
                Chunk chunk = Chunk.generate(world, chunkX, chunkZ);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        columns++;
                        if (hasStandingRoom(chunk, x, z)) {
                            walkable++;
                        }
                        for (int y = 5; y < 123; y++) {
                            if (chunk.getBlock(x, y, z) == Block.AIR.id()) {
                                openBlocks++;
                            }
                        }
                    }
                }
            }
        }

        // The old generator carved isolated bubbles out of solid rock; this asks for the
        // opposite - most of the middle of the world is open space you can walk into.
        double openFraction = openBlocks / (double) (columns * 118);
        assertTrue(openFraction > 0.25,
                "the Nether should be mostly open cavern, was " + (int) (openFraction * 100) + "%");
        assertTrue(walkable > columns / 2,
                "most columns should have a floor with headroom, had " + walkable + "/" + columns);
    }

    @Test
    void lavaSeaSitsInTheBottomAndTheRoofIsSealed(@TempDir Path dir) {
        World world = nether(dir);
        Chunk chunk = Chunk.generate(world, 0, 0);

        int lava = 0;
        int glowstone = 0;
        int quartz = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(Block.BEDROCK.id(), chunk.getBlock(x, 0, z), "bedrock floor");
                assertEquals(Block.BEDROCK.id(), chunk.getBlock(x, 127, z), "bedrock roof");
                for (int y = 1; y < 127; y++) {
                    short block = chunk.getBlock(x, y, z);
                    if (block == Block.LAVA.id()) {
                        lava++;
                        assertTrue(y <= NetherGenerator.LAVA_LEVEL,
                                "lava should not be above the sea level, found at y=" + y);
                    }
                    if (block == Block.GLOWSTONE.id()) glowstone++;
                    if (block == Block.QUARTZ_ORE.id()) quartz++;
                }
            }
        }

        assertTrue(lava > 0, "there is a lava sea down low");
        assertTrue(glowstone > 0, "glowstone lights the cavern");
        assertTrue(quartz > 0, "quartz runs through the netherrack");
    }

    /** A solid block with three blocks of air over it, somewhere in the column. */
    private static boolean hasStandingRoom(Chunk chunk, int x, int z) {
        for (int y = 5; y < 118; y++) {
            short ground = chunk.getBlock(x, y, z);
            if (ground == Block.AIR.id() || ground == Block.LAVA.id()) {
                continue;
            }
            if (chunk.getBlock(x, y + 1, z) == Block.AIR.id()
                    && chunk.getBlock(x, y + 2, z) == Block.AIR.id()
                    && chunk.getBlock(x, y + 3, z) == Block.AIR.id()) {
                return true;
            }
        }
        return false;
    }
}
