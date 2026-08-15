package net.vibmc.world.gen.structure;

import net.vibmc.world.Block;
import net.vibmc.world.Dimension;
import net.vibmc.world.World;

/**
 * Builds the portals that make the Nether and End reachable, and the platforms that keep
 * players from arriving inside rock or over the void.
 *
 * <p>These are stamped into a live world rather than produced by a chunk generator: a
 * portal has to exist at a known, safe location on both sides of a trip, which is a
 * property of the journey rather than of any one chunk.
 */
public final class PortalBuilder {
    /** Interior height of a portal, matching vanilla's minimum frame. */
    private static final int PORTAL_HEIGHT = 3;
    private static final int PORTAL_WIDTH = 2;

    private PortalBuilder() {
    }

    /**
     * Builds an obsidian-framed portal with its base at {@code (x, y, z)}, oriented along
     * the X axis, and clears the space in front of it so a player can step out.
     */
    public static void buildNetherPortal(World world, int x, int y, int z) {
        // Floor, so the portal is never left hanging over a drop.
        for (int dx = -1; dx <= PORTAL_WIDTH; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlock(x + dx, y - 1, z + dz, Block.OBSIDIAN.id());
            }
        }

        // Frame: uprights on both sides, lintel across the top.
        for (int dy = 0; dy <= PORTAL_HEIGHT; dy++) {
            world.setBlock(x - 1, y + dy, z, Block.OBSIDIAN.id());
            world.setBlock(x + PORTAL_WIDTH, y + dy, z, Block.OBSIDIAN.id());
        }
        for (int dx = 0; dx < PORTAL_WIDTH; dx++) {
            world.setBlock(x + dx, y + PORTAL_HEIGHT, z, Block.OBSIDIAN.id());
            world.setBlock(x + dx, y - 1, z, Block.OBSIDIAN.id());
        }

        // The portal surface itself.
        for (int dx = 0; dx < PORTAL_WIDTH; dx++) {
            for (int dy = 0; dy < PORTAL_HEIGHT; dy++) {
                world.setBlock(x + dx, y + dy, z, Block.NETHER_PORTAL.id());
            }
        }

        clearArrivalSpace(world, x, y, z);
    }

    /**
     * The End's exit portal: a ring of portal frames around a small end-stone platform,
     * with the portal blocks in the middle. Standing in it returns you to the overworld.
     */
    public static void buildEndExitPortal(World world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlock(x + dx, y - 1, z + dz, Block.END_STONE.id());
            }
        }
        // Frame ring at the platform edge.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (edge) {
                    world.setBlock(x + dx, y, z + dz, Block.END_PORTAL_FRAME.id());
                }
            }
        }
        // Portal interior.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlock(x + dx, y, z + dz, Block.END_PORTAL.id());
            }
        }
    }

    /**
     * The obsidian platform a player materialises on when entering the End, cleared of
     * anything above it. Vanilla builds this at a fixed spot for exactly this reason.
     */
    public static void buildEndArrivalPlatform(World world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlock(x + dx, y, z + dz, Block.OBSIDIAN.id());
                for (int dy = 1; dy <= 4; dy++) {
                    world.setBlock(x + dx, y + dy, z + dz, Block.AIR.id());
                }
            }
        }
    }

    /** Hollows out the space around a portal so arriving players are not buried. */
    private static void clearArrivalSpace(World world, int x, int y, int z) {
        for (int dx = -2; dx <= PORTAL_WIDTH + 1; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= PORTAL_HEIGHT + 1; dy++) {
                    // Never carve away the frame or the portal surface itself.
                    if (dz == 0 && dx >= -1 && dx <= PORTAL_WIDTH) {
                        continue;
                    }
                    world.setBlock(x + dx, y + dy, z + dz, Block.AIR.id());
                }
                // Give it a floor to stand on.
                world.setBlock(x + dx, y - 1, z + dz, Block.OBSIDIAN.id());
            }
        }
    }

    /**
     * Finds a safe height to put a portal at in the given dimension, then builds it.
     *
     * @return the Y the portal's base was placed at
     */
    public static int buildLinkedPortal(World world, int x, int z) {
        int y = safePortalY(world, x, z);
        buildNetherPortal(world, x, y, z);
        return y;
    }

    /**
     * Picks a Y for a portal. In the Nether that means a gap below the roof rather than
     * the surface, since "the surface" there is the bedrock ceiling.
     */
    private static int safePortalY(World world, int x, int z) {
        if (world.dimension() == Dimension.NETHER) {
            // Look for open space in the habitable band, working up from just above the
            // lava so players do not arrive standing in it.
            for (int y = 40; y < 100; y++) {
                if (isClearFor(world, x, y, z)) {
                    return y;
                }
            }
            return 64;
        }
        int surface = world.getHighestSolidY(x, z) + 1;
        return Math.max(world.getSeaLevel() + 1, surface);
    }

    private static boolean isClearFor(World world, int x, int y, int z) {
        for (int dy = 0; dy <= PORTAL_HEIGHT + 1; dy++) {
            for (int dx = -1; dx <= PORTAL_WIDTH; dx++) {
                if (world.getBlock(x + dx, y + dy, z) != Block.AIR.id()) {
                    return false;
                }
            }
        }
        return true;
    }
}
