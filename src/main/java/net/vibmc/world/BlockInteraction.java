package net.vibmc.world;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.item.ItemBlocks;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;
import net.vibmc.player.GameMode;
import net.vibmc.player.PlayerManager;
import net.vibmc.server.VibMC;
import net.vibmc.world.gen.structure.PortalBuilder;

/**
 * What happens when a player hits or right-clicks a block.
 *
 * <p>The server owns the world, so every one of these is decided here and the result is
 * sent back out as a block change - including the rejections, which is how a client that
 * has already drawn the block it thought it placed gets corrected.
 *
 * <p>This is deliberately a small survival vocabulary rather than a re-implementation of
 * vanilla's: break a block and it goes in your inventory, place it and it comes back out,
 * pour water on lava and you get obsidian, and flint and steel lights a frame. That chain
 * is exactly what a player needs to build their own way into the Nether.
 */
public final class BlockInteraction {
    /** How far a player may reach, generously, before the interaction is thrown out. */
    private static final double MAX_REACH = 7.0;

    private BlockInteraction() {
    }

    /** Breaks the block at these coordinates, if the player is allowed to. */
    public static void breakBlock(PlayerEntity player, int x, int y, int z) {
        World world = player.getWorld();
        short existing = world.getBlock(x, y, z);
        if (existing == Block.AIR.id()) {
            return;
        }
        if (!inReach(player, x, y, z) || ItemBlocks.isUnbreakable(existing)) {
            // Put the client's view back the way the world actually is.
            sendCorrection(player, world, x, y, z);
            return;
        }

        world.setBlock(x, y, z, Block.AIR.id());
        broadcast(world, x, y, z, Block.AIR.id());

        if (player.getGameModeEnum() == GameMode.CREATIVE) {
            return;
        }
        ItemType drop = ItemBlocks.dropFor(existing);
        if (drop != null) {
            player.addItem(new ItemStack(drop, 1));
            sendInventory(player);
        }
    }

    /**
     * Right-click on a block: places what the player is holding, or uses it on the block.
     *
     * @param face the clicked face, in the protocol's order (0 = bottom, 1 = top,
     *             2 = north, 3 = south, 4 = west, 5 = east)
     */
    public static void useItem(PlayerEntity player, int x, int y, int z, int face) {
        if (!inReach(player, x, y, z)) {
            return;
        }
        ItemStack held = heldItem(player);
        if (held == null || held.isEmpty()) {
            return;
        }

        World world = player.getWorld();
        int[] target = offset(x, y, z, face);
        ItemType type = held.getType();

        if (type == ItemType.FLINT_AND_STEEL) {
            lightPortal(player, world, target[0], target[1], target[2]);
            return;
        }
        if (type == ItemType.WATER_BUCKET) {
            useWaterBucket(player, world, x, y, z, target);
            return;
        }
        if (type == ItemType.LAVA_BUCKET) {
            if (placeBlock(player, world, target, Block.LAVA.id())) {
                replaceHeld(player, new ItemStack(ItemType.BUCKET, 1));
            }
            return;
        }

        if (type == ItemType.ENDER_EYE) {
            placeEye(player, world, x, y, z);
            return;
        }

        Block block = ItemBlocks.blockFor(type);
        if (block == null) {
            return;
        }
        if (type == ItemType.END_PORTAL_FRAME) {
            // A frame points back at whoever placed it, the way vanilla's does.
            block = Block.frameFacing(facingFromYaw(player.getYaw()) + 2);
        }
        if (placeBlock(player, world, target, block.id())) {
            consumeHeld(player);
        }
    }

    /**
     * Puts an eye of ender into a frame, and opens the portal if that was the twelfth.
     *
     * <p>The eye goes into the block that was clicked rather than the space in front of
     * it - it is being fitted into the frame, not placed next to it.
     */
    private static void placeEye(PlayerEntity player, World world, int x, int y, int z) {
        short existing = world.getBlock(x, y, z);
        if (!Block.isEndPortalFrame(existing)) {
            return;
        }
        if (Block.frameHasEye(existing)) {
            return;
        }

        short eyed = Block.frameWithEye(existing);
        world.setBlock(x, y, z, eyed);
        broadcast(world, x, y, z, eyed);
        consumeHeld(player);

        int[] portal = PortalBuilder.activateEndPortal(world, x, y, z);
        if (portal == null) {
            return;
        }
        for (int bx = portal[0]; bx <= portal[3]; bx++) {
            for (int bz = portal[2]; bz <= portal[5]; bz++) {
                broadcast(world, bx, portal[1], bz, Block.END_PORTAL.id());
            }
        }
        player.sendMessage("{\"text\":\"§5The portal opens.\"}");
    }

    /** The horizontal direction a yaw points: 0 south, 1 west, 2 north, 3 east. */
    private static int facingFromYaw(float yaw) {
        return Math.floorMod((int) Math.floor(yaw / 90.0 + 0.5), 4);
    }

    /**
     * Water on lava makes obsidian - the only way to get any, now that spawn no longer
     * comes with a portal. Anywhere else the bucket just puts water down.
     */
    private static void useWaterBucket(PlayerEntity player, World world, int x, int y, int z,
                                       int[] target) {
        if (world.getBlock(x, y, z) == Block.LAVA.id()) {
            world.setBlock(x, y, z, Block.OBSIDIAN.id());
            broadcast(world, x, y, z, Block.OBSIDIAN.id());
            replaceHeld(player, new ItemStack(ItemType.BUCKET, 1));
            return;
        }
        if (world.getBlock(target[0], target[1], target[2]) == Block.LAVA.id()) {
            world.setBlock(target[0], target[1], target[2], Block.OBSIDIAN.id());
            broadcast(world, target[0], target[1], target[2], Block.OBSIDIAN.id());
            replaceHeld(player, new ItemStack(ItemType.BUCKET, 1));
            return;
        }
        if (placeBlock(player, world, target, Block.WATER.id())) {
            replaceHeld(player, new ItemStack(ItemType.BUCKET, 1));
        }
    }

    /** Tries to light a nether portal in the frame around this spot. */
    private static void lightPortal(PlayerEntity player, World world, int x, int y, int z) {
        int[] filled = PortalBuilder.ignite(world, x, y, z);
        if (filled == null) {
            player.sendMessage("{\"text\":\"§7Nothing to light here - a portal needs an obsidian"
                    + " frame at least 4 by 5 with the inside empty.\"}");
            return;
        }
        for (int bx = filled[0]; bx <= filled[3]; bx++) {
            for (int by = filled[1]; by <= filled[4]; by++) {
                for (int bz = filled[2]; bz <= filled[5]; bz++) {
                    broadcast(world, bx, by, bz, world.getBlock(bx, by, bz));
                }
            }
        }
    }

    /** Places a block, refusing to bury the player who placed it. */
    private static boolean placeBlock(PlayerEntity player, World world, int[] target, short block) {
        int x = target[0];
        int y = target[1];
        int z = target[2];
        if (y < 0 || y >= Chunk.WORLD_HEIGHT) {
            return false;
        }
        short existing = world.getBlock(x, y, z);
        if (existing != Block.AIR.id() && existing != Block.WATER.id()
                && existing != Block.LAVA.id()) {
            sendCorrection(player, world, x, y, z);
            return false;
        }
        if (occupiedByPlayer(player, x, y, z)) {
            sendCorrection(player, world, x, y, z);
            return false;
        }
        world.setBlock(x, y, z, block);
        broadcast(world, x, y, z, block);
        return true;
    }

    /** True if the block would appear inside the placing player's own two-block column. */
    private static boolean occupiedByPlayer(PlayerEntity player, int x, int y, int z) {
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());
        int py = (int) Math.floor(player.getY());
        return px == x && pz == z && (py == y || py + 1 == y);
    }

    private static boolean inReach(PlayerEntity player, int x, int y, int z) {
        double dx = player.getX() - (x + 0.5);
        double dy = player.getY() + 1.6 - (y + 0.5);
        double dz = player.getZ() - (z + 0.5);
        return dx * dx + dy * dy + dz * dz <= MAX_REACH * MAX_REACH;
    }

    private static int[] offset(int x, int y, int z, int face) {
        switch (face) {
            case 0: return new int[]{x, y - 1, z};
            case 1: return new int[]{x, y + 1, z};
            case 2: return new int[]{x, y, z - 1};
            case 3: return new int[]{x, y, z + 1};
            case 4: return new int[]{x - 1, y, z};
            case 5: return new int[]{x + 1, y, z};
            default: return new int[]{x, y + 1, z};
        }
    }

    private static ItemStack heldItem(PlayerEntity player) {
        return player.getInventory().getSlot(player.getHeldItemSlot());
    }

    private static void consumeHeld(PlayerEntity player) {
        if (player.getGameModeEnum() == GameMode.CREATIVE) {
            return;
        }
        player.getInventory().removeItem(player.getHeldItemSlot(), 1);
        sendHeldSlot(player);
    }

    /** Swaps the held stack for another item, as emptying a bucket does. */
    private static void replaceHeld(PlayerEntity player, ItemStack replacement) {
        if (player.getGameModeEnum() == GameMode.CREATIVE) {
            return;
        }
        player.getInventory().setSlot(player.getHeldItemSlot(), replacement);
        sendHeldSlot(player);
    }

    /** One slot changed, so only that slot is sent. */
    private static void sendHeldSlot(PlayerEntity player) {
        PlayerManager players = playerManager();
        if (players != null) {
            players.sendSlot(player, player.getHeldItemSlot());
        }
    }

    private static void sendCorrection(PlayerEntity player, World world, int x, int y, int z) {
        PlayerManager players = playerManager();
        if (players != null) {
            players.broadcastBlockChange(world, x, y, z, world.getBlock(x, y, z));
        }
    }

    private static void broadcast(World world, int x, int y, int z, short block) {
        PlayerManager players = playerManager();
        if (players != null) {
            players.broadcastBlockChange(world, x, y, z, block);
        }
    }

    private static void sendInventory(PlayerEntity player) {
        PlayerManager players = playerManager();
        if (players != null) {
            players.sendInventory(player);
        }
    }

    private static PlayerManager playerManager() {
        VibMC server = VibMC.getInstance();
        return server == null ? null : server.getPlayerManager();
    }
}
