package net.vibmc.world;

import net.vibmc.world.gen.TerrainGenerator;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainGenerationTest {
    private static final long SEED = 1227398065945640716L;

    private World overworld(Path dir) {
        String name = dir.resolve("world").toString();
        return new World(SEED, name, Dimension.OVERWORLD, new WorldStorage(name));
    }

    @Test
    void terrainSitsAroundVanillaSeaLevel(@TempDir Path dir) {
        World world = overworld(dir);
        TerrainGenerator terrain = new TerrainGenerator(world.seed());

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = -500; x <= 500; x += 7) {
            for (int z = -500; z <= 500; z += 7) {
                int height = terrain.surfaceHeight(x, z);
                min = Math.min(min, height);
                max = Math.max(max, height);
            }
        }

        assertTrue(min < TerrainGenerator.SEA_LEVEL,
                "some terrain should sit below sea level so there are oceans, got min " + min);
        assertTrue(max > TerrainGenerator.SEA_LEVEL + 10,
                "some terrain should rise well above sea level, got max " + max);
        assertTrue(max - min > 20, "the world should have real elevation change, got " + (max - min));
    }

    @Test
    void columnsAreLayeredBedrockStoneSoilSurface(@TempDir Path dir) {
        World world = overworld(dir);
        TerrainGenerator terrain = new TerrainGenerator(world.seed());

        // Find a dry column so we are inspecting land rather than seabed.
        int foundX = Integer.MIN_VALUE;
        int foundZ = 0;
        for (int x = 0; x < 400 && foundX == Integer.MIN_VALUE; x++) {
            for (int z = 0; z < 400; z++) {
                if (terrain.surfaceHeight(x, z) > TerrainGenerator.SEA_LEVEL + 4) {
                    foundX = x;
                    foundZ = z;
                    break;
                }
            }
        }
        assertNotEquals(Integer.MIN_VALUE, foundX, "expected to find dry land somewhere");

        Chunk chunk = world.getChunk(Math.floorDiv(foundX, 16), Math.floorDiv(foundZ, 16));
        int localX = Math.floorMod(foundX, 16);
        int localZ = Math.floorMod(foundZ, 16);
        int surface = terrain.surfaceHeight(foundX, foundZ);

        assertEquals(Block.BEDROCK.id(), chunk.getBlock(localX, 0, localZ),
                "y=0 is always bedrock");
        assertEquals(Block.AIR.id(), chunk.getBlock(localX, surface + 1, localZ),
                "the block above the surface is open air");

        short surfaceBlock = chunk.getBlock(localX, surface, localZ);
        assertTrue(surfaceBlock == Block.GRASS.id() || surfaceBlock == Block.SAND.id()
                        || surfaceBlock == Block.SNOW.id(),
                "dry land is topped with grass, sand or snow but was " + surfaceBlock);
    }

    @Test
    void oceansFillWithWaterUpToSeaLevel(@TempDir Path dir) {
        World world = overworld(dir);
        TerrainGenerator terrain = new TerrainGenerator(world.seed());

        int foundX = Integer.MIN_VALUE;
        int foundZ = 0;
        for (int x = 0; x < 600 && foundX == Integer.MIN_VALUE; x++) {
            for (int z = 0; z < 600; z++) {
                if (terrain.surfaceHeight(x, z) < TerrainGenerator.SEA_LEVEL - 3) {
                    foundX = x;
                    foundZ = z;
                    break;
                }
            }
        }
        assertNotEquals(Integer.MIN_VALUE, foundX, "expected to find ocean somewhere");

        Chunk chunk = world.getChunk(Math.floorDiv(foundX, 16), Math.floorDiv(foundZ, 16));
        int localX = Math.floorMod(foundX, 16);
        int localZ = Math.floorMod(foundZ, 16);

        assertEquals(Block.WATER.id(), chunk.getBlock(localX, TerrainGenerator.SEA_LEVEL, localZ),
                "the sea surface is water");
        assertEquals(Block.AIR.id(), chunk.getBlock(localX, TerrainGenerator.SEA_LEVEL + 1, localZ),
                "water stops at sea level");
    }

    @Test
    void generationIsDeterministicForASeed(@TempDir Path dir) {
        World first = new World(SEED, dir.resolve("a").toString(), Dimension.OVERWORLD,
                new WorldStorage(dir.resolve("a").toString()));
        World second = new World(SEED, dir.resolve("b").toString(), Dimension.OVERWORLD,
                new WorldStorage(dir.resolve("b").toString()));

        Chunk one = Chunk.generate(first, 3, -5);
        Chunk two = Chunk.generate(second, 3, -5);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128; y++) {
                    assertEquals(one.getBlock(x, y, z), two.getBlock(x, y, z),
                            "same seed must produce the same block at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void differentSeedsProduceDifferentWorlds(@TempDir Path dir) {
        World first = new World(1L, dir.resolve("a").toString(), Dimension.OVERWORLD,
                new WorldStorage(dir.resolve("a").toString()));
        World second = new World(2L, dir.resolve("b").toString(), Dimension.OVERWORLD,
                new WorldStorage(dir.resolve("b").toString()));

        boolean anyDifference = false;
        for (int x = 0; x < 16 && !anyDifference; x++) {
            for (int z = 0; z < 16; z++) {
                if (new TerrainGenerator(first.seed()).surfaceHeight(x, z)
                        != new TerrainGenerator(second.seed()).surfaceHeight(x, z)) {
                    anyDifference = true;
                    break;
                }
            }
        }
        assertTrue(anyDifference, "different seeds should generate different terrain");
    }

    @Test
    void neighbouringChunksAgreeAtTheirBorder(@TempDir Path dir) {
        World world = overworld(dir);
        TerrainGenerator terrain = new TerrainGenerator(world.seed());

        // The surface formula is a pure function of world coordinates, so the last column
        // of one chunk and the first of the next must describe the same ground - that is
        // what stops visible seams at chunk borders.
        for (int z = 0; z < 16; z++) {
            int leftEdge = terrain.surfaceHeight(15, z);
            int rightEdge = terrain.surfaceHeight(16, z);
            assertTrue(Math.abs(leftEdge - rightEdge) <= 3,
                    "terrain should be continuous across a chunk border, got "
                            + leftEdge + " then " + rightEdge);
        }
    }

    @Test
    void generationWorksAtNegativeCoordinates(@TempDir Path dir) {
        World world = overworld(dir);
        Chunk chunk = world.getChunk(-7, -12);

        assertEquals(Block.BEDROCK.id(), chunk.getBlock(0, 0, 0),
                "negative chunks still get their bedrock floor");
        assertTrue(chunk.highestBlock(8, 8) > 0,
                "negative chunks generate real terrain, not empty space");
    }

    @Test
    void cavesAndOresExistUnderground(@TempDir Path dir) {
        World world = overworld(dir);

        int air = 0;
        int coal = 0;
        int iron = 0;
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                Chunk chunk = world.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 6; y < 60; y++) {
                            short block = chunk.getBlock(x, y, z);
                            if (block == Block.AIR.id()) air++;
                            else if (block == Block.COAL_ORE.id()) coal++;
                            else if (block == Block.IRON_ORE.id()) iron++;
                        }
                    }
                }
            }
        }

        assertTrue(air > 0, "caves should hollow out some underground space");
        assertTrue(coal > 0, "coal should generate underground");
        assertTrue(iron > 0, "iron should generate underground");
    }
}
