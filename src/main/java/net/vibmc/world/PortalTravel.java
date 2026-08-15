package net.vibmc.world;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.VibMC;
import net.vibmc.world.gen.EndGenerator;
import net.vibmc.world.gen.structure.PortalBuilder;

/**
 * Moving players between dimensions.
 *
 * <p>Standing in a portal block starts a trip. The destination is worked out the way
 * vanilla does it - Nether coordinates are the overworld's divided by eight, the End
 * always uses its fixed arrival platform - and if there is no portal waiting on the far
 * side, one is built before the player arrives.
 */
public final class PortalTravel {
    /** Ticks a player must stand in a portal before it fires, so they can step back out. */
    public static final int ACTIVATION_TICKS = 40;
    /** Ticks after arriving during which the portal underfoot is ignored. */
    public static final int COOLDOWN_TICKS = 100;

    /** Where players materialise in the End, matching vanilla's fixed platform. */
    public static final int END_PLATFORM_X = 100;
    public static final int END_PLATFORM_Y = EndGenerator.ISLAND_SURFACE;
    public static final int END_PLATFORM_Z = 0;

    private PortalTravel() {
    }

    /** The block a player is standing in, used to decide whether a portal is active. */
    public static short blockAt(PlayerEntity player) {
        return player.getWorld().getBlock(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ()));
    }

    /** True when the player is inside a portal of any kind. */
    public static boolean inPortal(PlayerEntity player) {
        short block = blockAt(player);
        return block == Block.NETHER_PORTAL.id() || block == Block.END_PORTAL.id();
    }

    /** Where a player standing in a portal should end up, or null if they are not in one. */
    public static Dimension destinationFor(PlayerEntity player) {
        short block = blockAt(player);
        Dimension current = player.getWorld().dimension();
        if (block == Block.NETHER_PORTAL.id()) {
            return current == Dimension.NETHER ? Dimension.OVERWORLD : Dimension.NETHER;
        }
        if (block == Block.END_PORTAL.id()) {
            return current == Dimension.END ? Dimension.OVERWORLD : Dimension.END;
        }
        return null;
    }

    /**
     * Sends a player to another dimension, preparing the destination first.
     *
     * @return true if the trip happened
     */
    public static boolean travel(PlayerEntity player, Dimension destination) {
        WorldManager worlds = VibMC.getInstance().getWorldManager();
        World target = worlds.getWorld(destination);
        if (target == null || target == player.getWorld()) {
            return false;
        }

        double[] position = arrivalPosition(player, target, destination);
        player.changeDimension(target, position[0], position[1], position[2]);
        return true;
    }

    /**
     * Works out where in the target world the player lands, building whatever they need to
     * arrive safely - a linked portal, or the End's platform.
     */
    private static double[] arrivalPosition(PlayerEntity player, World target, Dimension destination) {
        if (destination == Dimension.END) {
            PortalBuilder.buildEndArrivalPlatform(target, END_PLATFORM_X, END_PLATFORM_Y, END_PLATFORM_Z);
            return new double[]{END_PLATFORM_X + 0.5, END_PLATFORM_Y + 1, END_PLATFORM_Z + 0.5};
        }

        Dimension from = player.getWorld().dimension();
        if (from == Dimension.END) {
            // Leaving the End drops you at the overworld spawn, as vanilla does.
            int x = 0;
            int z = 0;
            int y = target.getHighestSolidY(x, z) + 1;
            return new double[]{x + 0.5, y, z + 0.5};
        }

        // Nether travel: coordinates scale by 8 in the direction of travel.
        int x;
        int z;
        if (destination == Dimension.NETHER) {
            x = (int) Math.floor(player.getX()) / Dimension.NETHER_SCALE;
            z = (int) Math.floor(player.getZ()) / Dimension.NETHER_SCALE;
        } else {
            x = (int) Math.floor(player.getX()) * Dimension.NETHER_SCALE;
            z = (int) Math.floor(player.getZ()) * Dimension.NETHER_SCALE;
        }

        int existing = findExistingPortal(target, x, z);
        int y = existing >= 0 ? existing : PortalBuilder.buildLinkedPortal(target, x, z);
        // Step out of the portal itself so the return trip does not fire immediately.
        return new double[]{x + 0.5, y, z + 1.5};
    }

    /**
     * Looks for a portal already standing near the target coordinates, so a round trip
     * reuses the same pair rather than littering the world with new ones.
     *
     * @return the portal's base Y, or -1 if there is none nearby
     */
    private static int findExistingPortal(World target, int x, int z) {
        int radius = 8;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = 1; y < Chunk.WORLD_HEIGHT - 1; y++) {
                    if (target.getBlock(x + dx, y, z + dz) == Block.NETHER_PORTAL.id()) {
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Makes sure the spawn area of a world that predates portals still has the portal it
     * needs, so an existing save gains a way into the other dimensions instead of being
     * stuck without one.
     */
    public static void ensureSpawnPortal(World overworld) {
        int x = 0;
        int z = 0;
        if (findExistingPortal(overworld, x, z) >= 0) {
            return;
        }
        PortalBuilder.buildLinkedPortal(overworld, x, z);
    }

    /**
     * Makes sure the End has its exit portal on the central island. Like the spawn portal,
     * this runs for existing saves too, so a world created before the End existed still
     * gets a way back out of it.
     */
    public static void ensureEndExitPortal(World end) {
        int x = 0;
        int z = 0;
        int y = EndGenerator.ISLAND_SURFACE + 1;
        if (end.getBlock(x, y, z) == Block.END_PORTAL.id()) {
            return;
        }
        PortalBuilder.buildEndExitPortal(end, x, y, z);
    }
}
