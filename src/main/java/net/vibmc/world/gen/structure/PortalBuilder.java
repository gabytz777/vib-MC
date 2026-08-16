package net.vibmc.world.gen.structure;

import net.vibmc.world.Block;
import net.vibmc.world.Dimension;
import net.vibmc.world.World;
import net.vibmc.world.gen.NetherGenerator;

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
        // Frame ring at the platform edge: twelve frames, no corners, each already holding
        // an eye - this portal is lit, so it should look the part.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!isRingPosition(dx, dz)) {
                    continue;
                }
                world.setBlock(x + dx, y, z + dz,
                        Block.frameWithEye(Block.frameFacing(facingInward(dx, dz)).id()));
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
     * The twelve frame positions of an end portal: everything two out from the middle,
     * corners excluded.
     */
    private static boolean isRingPosition(int dx, int dz) {
        int reach = Math.max(Math.abs(dx), Math.abs(dz));
        return reach == 2 && !(Math.abs(dx) == 2 && Math.abs(dz) == 2);
    }

    /** Which way a frame at this ring position has to look to face the middle. */
    private static int facingInward(int dx, int dz) {
        if (dz == -2) {
            return 0; // sitting to the north, looking south
        }
        if (dz == 2) {
            return 2; // north
        }
        if (dx == -2) {
            return 3; // east
        }
        return 1;     // west
    }

    /**
     * Completes an end portal when the last eye of ender goes in.
     *
     * <p>An eye can be placed in any frame, so this looks at every 3x3 opening the frame
     * just filled could belong to rather than assuming where the middle is. Facing is not
     * part of the test: the frames are placed by a player, and refusing to open a portal
     * ringed by twelve eyes because one block points the wrong way is the sort of thing
     * that makes people think the server is broken.
     *
     * @return {@code {minX, y, minZ, maxX, y, maxZ}} of the portal surface, or null if the
     *         ring around this frame is not finished
     */
    public static int[] activateEndPortal(World world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int centreX = x + dx;
                int centreZ = z + dz;
                if (!ringIsComplete(world, centreX, y, centreZ)) {
                    continue;
                }
                for (int ix = -1; ix <= 1; ix++) {
                    for (int iz = -1; iz <= 1; iz++) {
                        world.setBlock(centreX + ix, y, centreZ + iz, Block.END_PORTAL.id());
                    }
                }
                return new int[]{centreX - 1, y, centreZ - 1, centreX + 1, y, centreZ + 1};
            }
        }
        return null;
    }

    private static boolean ringIsComplete(World world, int centreX, int y, int centreZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!isRingPosition(dx, dz)) {
                    continue;
                }
                if (!Block.frameHasEye(world.getBlock(centreX + dx, y, centreZ + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The obsidian platform a player materialises on when entering the End, cleared of
     * anything above it. Vanilla builds this at a fixed spot for exactly this reason.
     */
    public static void buildEndArrivalPlatform(World world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlock(x + dx, y, z + dz, Block.OBSIDIAN.id());
                // Clear well above head height: the generator keeps this patch of the End
                // empty, but an old save may already have rock overhead.
                for (int dy = 1; dy <= 20; dy++) {
                    world.setBlock(x + dx, y + dy, z + dz, Block.AIR.id());
                }
            }
        }
    }

    /**
     * Lights a portal in a frame a player has built, the way flint and steel does.
     *
     * <p>The clicked spot is treated as somewhere inside the opening: the search drops to
     * the bottom of the hole, measures how wide and how tall it is, and only fills it if
     * obsidian closes every side. Both orientations are tried, because a frame can run
     * east-west or north-south and the axis is part of the block state that gets written.
     *
     * @return {@code {minX, minY, minZ, maxX, maxY, maxZ}} of the blocks that were filled,
     *         or null if there was no valid frame here
     */
    public static int[] ignite(World world, int x, int y, int z) {
        int[] alongX = tryIgnite(world, x, y, z, 1, 0);
        if (alongX != null) {
            return alongX;
        }
        return tryIgnite(world, x, y, z, 0, 1);
    }

    /** Smallest and largest opening vanilla accepts, counting the interior only. */
    private static final int MIN_INTERIOR_WIDTH = 2;
    private static final int MIN_INTERIOR_HEIGHT = 3;
    private static final int MAX_INTERIOR = 21;

    private static int[] tryIgnite(World world, int x, int y, int z, int stepX, int stepZ) {
        // Drop to the floor of the opening.
        int bottom = y;
        while (bottom > 1 && isOpening(world, x, bottom - 1, z)) {
            bottom--;
        }
        if (world.getBlock(x, bottom - 1, z) != Block.OBSIDIAN.id()) {
            return null;
        }

        // Walk out to both frame uprights along the chosen axis.
        int back = 0;
        while (back < MAX_INTERIOR
                && isOpening(world, x - stepX * (back + 1), bottom, z - stepZ * (back + 1))) {
            back++;
        }
        int forward = 0;
        while (forward < MAX_INTERIOR
                && isOpening(world, x + stepX * (forward + 1), bottom, z + stepZ * (forward + 1))) {
            forward++;
        }
        int minX = x - stepX * back;
        int minZ = z - stepZ * back;
        int width = back + forward + 1;
        if (width < MIN_INTERIOR_WIDTH || width > MAX_INTERIOR) {
            return null;
        }
        if (!isFrame(world, minX - stepX, bottom, minZ - stepZ)
                || !isFrame(world, minX + stepX * width, bottom, minZ + stepZ * width)) {
            return null;
        }

        // Rise until the lintel, checking that every row is open and walled on both sides.
        int height = 0;
        while (height < MAX_INTERIOR && rowIsOpen(world, minX, bottom + height, minZ, stepX, stepZ, width)) {
            if (!isFrame(world, minX - stepX, bottom + height, minZ - stepZ)
                    || !isFrame(world, minX + stepX * width, bottom + height, minZ + stepZ * width)) {
                return null;
            }
            height++;
        }
        if (height < MIN_INTERIOR_HEIGHT || height > MAX_INTERIOR) {
            return null;
        }
        for (int i = 0; i < width; i++) {
            // Floor and lintel.
            if (!isFrame(world, minX + stepX * i, bottom - 1, minZ + stepZ * i)
                    || !isFrame(world, minX + stepX * i, bottom + height, minZ + stepZ * i)) {
                return null;
            }
        }

        // A frame running along X holds an X-axis portal surface, and vice versa.
        short surface = stepX != 0 ? Block.NETHER_PORTAL.id() : Block.NETHER_PORTAL_Z.id();
        for (int i = 0; i < width; i++) {
            for (int dy = 0; dy < height; dy++) {
                world.setBlock(minX + stepX * i, bottom + dy, minZ + stepZ * i, surface);
            }
        }
        return new int[]{minX, bottom, minZ,
                minX + stepX * (width - 1), bottom + height - 1, minZ + stepZ * (width - 1)};
    }

    /** Space a portal surface could occupy: empty, or already alight. */
    private static boolean isOpening(World world, int x, int y, int z) {
        short block = world.getBlock(x, y, z);
        return block == Block.AIR.id() || Block.isNetherPortal(block);
    }

    private static boolean isFrame(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == Block.OBSIDIAN.id();
    }

    private static boolean rowIsOpen(World world, int x, int y, int z, int stepX, int stepZ, int width) {
        for (int i = 0; i < width; i++) {
            if (!isOpening(world, x + stepX * i, y, z + stepZ * i)) {
                return false;
            }
        }
        return true;
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
            // Stand the portal on the cavern floor if there is one: arriving on solid
            // ground beats arriving on a shelf of obsidian hanging over the lava.
            for (int y = 100; y > NetherGenerator.LAVA_LEVEL + 2; y--) {
                short ground = world.getBlock(x, y, z);
                if (ground == Block.AIR.id() || ground == Block.LAVA.id()) {
                    continue;
                }
                if (isClearFor(world, x, y + 1, z)) {
                    return y + 1;
                }
            }
            // Nothing to stand on nearby - fall back to the first clear band and let the
            // builder lay its own floor.
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
