package net.vibmc.world;

import net.vibmc.world.gen.EndGenerator;
import net.vibmc.world.gen.structure.PortalBuilder;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionPortalTest {
    private static final long SEED = 4242L;

    private World world(Path dir, Dimension dimension) {
        String name = dir.resolve("w" + dimension.name()).toString();
        return new World(SEED, name, dimension, new WorldStorage(name));
    }

    @Test
    void dimensionsCarryTheirVanillaProtocolIds() {
        assertEquals(0, Dimension.OVERWORLD.protocolId());
        assertEquals(-1, Dimension.NETHER.protocolId());
        assertEquals(1, Dimension.END.protocolId());

        assertEquals(Dimension.NETHER, Dimension.byProtocolId(-1));
        assertEquals(Dimension.END, Dimension.byProtocolId(1));

        // Only the overworld has a sky; sending sky light for the others would light them
        // as though the sun reached underground.
        assertTrue(Dimension.OVERWORLD.hasSkyLight());
        assertTrue(!Dimension.NETHER.hasSkyLight());
        assertTrue(!Dimension.END.hasSkyLight());
    }

    @Test
    void netherHasBedrockFloorAndRoofWithNetherrackBetween(@TempDir Path dir) {
        World nether = world(dir, Dimension.NETHER);
        Chunk chunk = nether.getChunk(0, 0);

        assertEquals(Block.BEDROCK.id(), chunk.getBlock(8, 0, 8), "the Nether has a bedrock floor");
        assertEquals(Block.BEDROCK.id(), chunk.getBlock(8, 127, 8), "the Nether has a bedrock roof");

        int netherrack = 0;
        int lava = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 5; y < 120; y++) {
                    short block = chunk.getBlock(x, y, z);
                    if (block == Block.NETHERRACK.id()) netherrack++;
                    if (block == Block.LAVA.id()) lava++;
                }
            }
        }
        assertTrue(netherrack > 0, "the Nether is mostly netherrack");
        assertTrue(lava > 0, "the Nether has lava down low");
    }

    @Test
    void endHasASolidCentralIslandSurroundedByVoid(@TempDir Path dir) {
        World end = world(dir, Dimension.END);

        // The island players arrive on must be solid, or they fall straight into the void.
        assertEquals(Block.END_STONE.id(),
                end.getBlock(0, EndGenerator.ISLAND_SURFACE, 0),
                "the central End island is solid end stone");

        // Far from the origin the End should mostly be empty space.
        assertEquals(Block.AIR.id(), end.getBlock(6000, EndGenerator.ISLAND_SURFACE, 6000),
                "the End is void far from the central island");
    }

    @Test
    void netherPortalIsFramedInObsidianAroundPortalBlocks(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);
        int x = 0;
        int z = 0;
        int y = PortalBuilder.buildLinkedPortal(overworld, x, z);

        assertEquals(Block.NETHER_PORTAL.id(), overworld.getBlock(x, y, z),
                "the portal interior is portal blocks");
        assertEquals(Block.OBSIDIAN.id(), overworld.getBlock(x - 1, y, z),
                "the portal is framed in obsidian");
        assertEquals(Block.OBSIDIAN.id(), overworld.getBlock(x, y - 1, z),
                "the portal stands on obsidian");

        // A player stepping out must have somewhere to stand and room to stand in.
        assertEquals(Block.AIR.id(), overworld.getBlock(x, y, z + 1),
                "there is space to step out of the portal");
    }

    @Test
    void endExitPortalIsBuiltOnTheCentralIsland(@TempDir Path dir) {
        World end = world(dir, Dimension.END);
        PortalTravel.ensureEndExitPortal(end);

        int y = EndGenerator.ISLAND_SURFACE + 1;
        assertEquals(Block.END_PORTAL.id(), end.getBlock(0, y, 0),
                "the End's exit portal sits at the middle of the island");
        assertEquals(Block.END_PORTAL_FRAME.id(), end.getBlock(2, y, 0),
                "the exit portal is ringed with frames");

        // Running it twice must not double-build or move anything.
        PortalTravel.ensureEndExitPortal(end);
        assertEquals(Block.END_PORTAL.id(), end.getBlock(0, y, 0));
    }

    @Test
    void spawnPortalIsCreatedOnceForExistingWorlds(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);

        PortalTravel.ensureSpawnPortal(overworld);
        int firstY = findPortalY(overworld);
        assertNotEquals(-1, firstY, "a spawn portal should be built for a world without one");

        // A second pass finds the existing portal and leaves it alone.
        PortalTravel.ensureSpawnPortal(overworld);
        assertEquals(firstY, findPortalY(overworld), "the existing portal is reused, not rebuilt");
    }

    @Test
    void netherCoordinatesAreEightTimesSmaller() {
        // The scale is what makes a Nether trip cover eight times the overworld distance.
        assertEquals(8, Dimension.NETHER_SCALE);
        assertEquals(125, 1000 / Dimension.NETHER_SCALE);
    }

    private static int findPortalY(World world) {
        for (int y = 1; y < Chunk.WORLD_HEIGHT - 1; y++) {
            if (world.getBlock(0, y, 0) == Block.NETHER_PORTAL.id()) {
                return y;
            }
        }
        return -1;
    }
}
