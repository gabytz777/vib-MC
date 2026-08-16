package net.vibmc.world;

import net.vibmc.world.gen.EndGenerator;
import net.vibmc.world.gen.structure.PortalBuilder;
import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        // The exit portal is already lit, so its frames come with their eyes in.
        assertTrue(Block.isEndPortalFrame(end.getBlock(2, y, 0)),
                "the exit portal is ringed with frames");
        assertTrue(Block.frameHasEye(end.getBlock(2, y, 0)),
                "a lit portal's frames hold eyes");

        // Running it twice must not double-build or move anything.
        PortalTravel.ensureEndExitPortal(end);
        assertEquals(Block.END_PORTAL.id(), end.getBlock(0, y, 0));
    }

    @Test
    void spawnHasNoPortalOfItsOwn(@TempDir Path dir) {
        // Getting to the Nether is meant to be something the player builds, so generating
        // the spawn area must not leave a portal standing there.
        World overworld = world(dir, Dimension.OVERWORLD);
        overworld.getChunk(0, 0);

        assertEquals(-1, findPortalY(overworld), "spawn should not come with a portal");
    }

    @Test
    void portalBlocksCarryTheAxisTheClientNeeds() {
        // A portal with no axis is not a block state the vanilla client knows, and it
        // renders the whole thing as air - the frame looks built but never lights up.
        assertEquals((short) ((90 << 4) | 1), Block.stateIdOf(Block.NETHER_PORTAL.id()),
                "an east-west portal is axis X");
        assertEquals((short) ((90 << 4) | 2), Block.stateIdOf(Block.NETHER_PORTAL_Z.id()),
                "a north-south portal is axis Z");
        assertTrue(Block.isNetherPortal(Block.NETHER_PORTAL_Z.id()));
    }

    @Test
    void aPlayerBuiltFrameLightsAlongEitherAxis(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);

        int y = 80;
        buildEmptyFrame(overworld, 0, y, 0, true);
        assertNotNull(PortalBuilder.ignite(overworld, 0, y, 0), "an east-west frame lights");
        assertEquals(Block.NETHER_PORTAL.id(), overworld.getBlock(0, y, 0));
        assertEquals(Block.NETHER_PORTAL.id(), overworld.getBlock(1, y + 2, 0));

        buildEmptyFrame(overworld, 40, y, 40, false);
        assertNotNull(PortalBuilder.ignite(overworld, 40, y + 1, 40), "a north-south frame lights");
        assertEquals(Block.NETHER_PORTAL_Z.id(), overworld.getBlock(40, y, 41));
    }

    @Test
    void anIncompleteFrameDoesNotLight(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);

        int y = 80;
        buildEmptyFrame(overworld, 100, y, 100, true);
        // Knock a corner-adjacent frame block out: the opening is no longer enclosed.
        overworld.setBlock(100, y + 3, 100, Block.AIR.id());

        assertNull(PortalBuilder.ignite(overworld, 100, y, 100),
                "a frame with a hole in it is not a portal");
        assertEquals(Block.AIR.id(), overworld.getBlock(100, y, 100));
    }

    /**
     * A bare 4x5 obsidian frame with a 2x3 opening whose bottom-left interior corner is at
     * {@code (x, y, z)}, running east-west or north-south.
     */
    private static void buildEmptyFrame(World world, int x, int y, int z, boolean alongX) {
        int stepX = alongX ? 1 : 0;
        int stepZ = alongX ? 0 : 1;
        for (int i = -1; i <= 2; i++) {
            for (int dy = -1; dy <= 3; dy++) {
                boolean border = i == -1 || i == 2 || dy == -1 || dy == 3;
                short block = border ? Block.OBSIDIAN.id() : Block.AIR.id();
                world.setBlock(x + stepX * i, y + dy, z + stepZ * i, block);
            }
        }
    }

    @Test
    void arrivalPlatformIsASlabInTheVoidNotPartOfAnIsland(@TempDir Path dir) {
        World end = world(dir, Dimension.END);

        // The generator must leave this patch empty, or players arrive standing on an
        // island instead of on the platform.
        assertEquals(Block.AIR.id(),
                end.getBlock(PortalTravel.END_PLATFORM_X, EndGenerator.ISLAND_SURFACE,
                        PortalTravel.END_PLATFORM_Z),
                "no island grows where the platform goes");

        PortalBuilder.buildEndArrivalPlatform(end, PortalTravel.END_PLATFORM_X,
                PortalTravel.END_PLATFORM_Y, PortalTravel.END_PLATFORM_Z);

        assertEquals(Block.OBSIDIAN.id(),
                end.getBlock(PortalTravel.END_PLATFORM_X, PortalTravel.END_PLATFORM_Y,
                        PortalTravel.END_PLATFORM_Z),
                "the platform itself is obsidian");
        assertEquals(Block.AIR.id(),
                end.getBlock(PortalTravel.END_PLATFORM_X, PortalTravel.END_PLATFORM_Y + 1,
                        PortalTravel.END_PLATFORM_Z),
                "with room to stand on it");
        assertTrue(PortalTravel.END_PLATFORM_Y < EndGenerator.ISLAND_SURFACE,
                "and it sits below the island, out in the void");
    }

    @Test
    void obsidianTowersRingTheCentralIsland(@TempDir Path dir) {
        World end = world(dir, Dimension.END);

        int towerColumns = 0;
        int bedrockCaps = 0;
        for (int x = -34; x <= 34; x++) {
            for (int z = -34; z <= 34; z++) {
                if (end.getBlock(x, EndGenerator.ISLAND_SURFACE + 10, z) == Block.OBSIDIAN.id()) {
                    towerColumns++;
                }
                for (int y = EndGenerator.ISLAND_SURFACE + 17;
                     y <= EndGenerator.ISLAND_SURFACE + 36; y++) {
                    if (end.getBlock(x, y, z) == Block.BEDROCK.id()) {
                        bedrockCaps++;
                    }
                }
            }
        }

        assertTrue(towerColumns > 40, "towers should stand around the island, found "
                + towerColumns + " columns");
        assertTrue(bedrockCaps > 0, "each tower is capped with bedrock");
    }

    @Test
    void twelveEyesOpenAnEndPortal(@TempDir Path dir) {
        World overworld = world(dir, Dimension.OVERWORLD);
        int y = 70;

        // A ring of twelve empty frames: nothing should happen yet.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int reach = Math.max(Math.abs(dx), Math.abs(dz));
                if (reach != 2 || (Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
                    continue;
                }
                overworld.setBlock(dx, y, dz, Block.END_PORTAL_FRAME.id());
            }
        }
        assertNull(PortalBuilder.activateEndPortal(overworld, 2, y, 0),
                "an empty ring is not a portal");

        // Fill every frame but one.
        int lastX = 0;
        int lastZ = 2;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!Block.isEndPortalFrame(overworld.getBlock(dx, y, dz))) {
                    continue;
                }
                if (dx == lastX && dz == lastZ) {
                    continue;
                }
                overworld.setBlock(dx, y, dz, Block.frameWithEye(overworld.getBlock(dx, y, dz)));
            }
        }
        assertNull(PortalBuilder.activateEndPortal(overworld, -2, y, 0),
                "eleven eyes are not enough");

        // The twelfth eye opens it.
        overworld.setBlock(lastX, y, lastZ, Block.frameWithEye(overworld.getBlock(lastX, y, lastZ)));
        assertNotNull(PortalBuilder.activateEndPortal(overworld, lastX, y, lastZ));
        assertEquals(Block.END_PORTAL.id(), overworld.getBlock(0, y, 0), "the middle fills in");
        assertEquals(Block.END_PORTAL.id(), overworld.getBlock(1, y, 1), "so do the corners");
        assertEquals(Block.END_PORTAL_FRAME_EYE.id(), overworld.getBlock(0, y, -2),
                "the frames keep their eyes and their facing");
    }

    @Test
    void anEyedFrameIsStillAFrame() {
        short eyed = Block.frameWithEye(Block.END_PORTAL_FRAME_W.id());
        assertEquals(Block.END_PORTAL_FRAME_W_EYE.id(), eyed);
        assertTrue(Block.isEndPortalFrame(eyed));
        assertTrue(Block.frameHasEye(eyed));
        assertTrue(!Block.frameHasEye(Block.END_PORTAL_FRAME_W.id()));
        // Facing plus the eye bit, which is what the client draws the eye from.
        assertEquals((short) ((120 << 4) | 5), Block.stateIdOf(eyed));
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
