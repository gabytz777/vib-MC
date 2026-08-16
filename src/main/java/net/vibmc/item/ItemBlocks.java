package net.vibmc.item;

import net.vibmc.world.Block;

/**
 * The bridge between items in a player's hand and blocks in the world.
 *
 * <p>Kept apart from both enums on purpose: {@link ItemType} describes what the protocol
 * calls an item and {@link Block} describes what the world stores, and only placing and
 * breaking care that the two are related at all.
 */
public final class ItemBlocks {
    private ItemBlocks() {
    }

    /** The block this item places, or null if it is not a block item. */
    public static Block blockFor(ItemType item) {
        if (item == null) {
            return null;
        }
        switch (item) {
            case STONE:
                return Block.STONE;
            case GRASS_BLOCK:
                return Block.GRASS;
            case DIRT:
                return Block.DIRT;
            case PLANKS:
                return Block.PLANKS;
            case LOG:
                return Block.WOOD;
            case LEAVES:
                return Block.LEAVES;
            case SAND:
                return Block.SAND;
            case GRAVEL:
                return Block.GRAVEL;
            case CHEST:
                return Block.CHEST;
            case FURNACE:
                return Block.FURNACE;
            case CRAFTING_TABLE:
                return Block.CRAFTING_TABLE;
            case TRAPDOOR:
                return Block.TRAPDOOR;
            case COBBLESTONE:
                return Block.COBBLESTONE;
            case IRON_ORE:
                return Block.IRON_ORE;
            case COAL_ORE:
                return Block.COAL_ORE;
            case SNOW_BLOCK:
                return Block.SNOW;
            case NETHERRACK:
                return Block.NETHERRACK;
            case SOUL_SAND:
                return Block.SOUL_SAND;
            case GLOWSTONE:
                return Block.GLOWSTONE;
            case OBSIDIAN:
                return Block.OBSIDIAN;
            case END_STONE:
                return Block.END_STONE;
            case QUARTZ_ORE:
                return Block.QUARTZ_ORE;
            case END_PORTAL_FRAME:
                // Placed through BlockInteraction, which turns it to face the player.
                return Block.END_PORTAL_FRAME;
            case WATER:
                return Block.WATER;
            case LAVA:
                return Block.LAVA;
            default:
                return null;
        }
    }

    /**
     * What breaking a block puts in the player's inventory, or null if it drops nothing.
     *
     * <p>There are no tools or tool tiers yet, so a block simply drops itself. Bedrock and
     * the portal blocks drop nothing because they are not meant to be collected at all.
     */
    public static ItemType dropFor(short blockId) {
        Block block = Block.byId(blockId);
        switch (block) {
            case STONE:
            case ANDESITE:
            case DIORITE:
                return ItemType.COBBLESTONE;
            case GRASS:
            case DIRT:
                return ItemType.DIRT;
            case PLANKS:
                return ItemType.PLANKS;
            case WOOD:
                return ItemType.LOG;
            case SAND:
                return ItemType.SAND;
            case GRAVEL:
                return ItemType.GRAVEL;
            case CHEST:
                return ItemType.CHEST;
            case FURNACE:
                return ItemType.FURNACE;
            case CRAFTING_TABLE:
                return ItemType.CRAFTING_TABLE;
            case TRAPDOOR:
                return ItemType.TRAPDOOR;
            case COBBLESTONE:
                return ItemType.COBBLESTONE;
            case IRON_ORE:
                return ItemType.IRON_ORE;
            case COAL_ORE:
                return ItemType.COAL;
            case SNOW:
                return ItemType.SNOW_BLOCK;
            case NETHERRACK:
                return ItemType.NETHERRACK;
            case SOUL_SAND:
                return ItemType.SOUL_SAND;
            case GLOWSTONE:
                return ItemType.GLOWSTONE;
            case OBSIDIAN:
                return ItemType.OBSIDIAN;
            case END_STONE:
                return ItemType.END_STONE;
            case QUARTZ_ORE:
                return ItemType.QUARTZ;
            default:
                // Every orientation of an end portal frame gives the frame back, so a
                // misplaced one can be picked up instead of being stuck there forever.
                if (Block.isEndPortalFrame(blockId)) {
                    return ItemType.END_PORTAL_FRAME;
                }
                return null;
        }
    }

    /** Blocks that refuse to break, whatever the player is holding. */
    public static boolean isUnbreakable(short blockId) {
        return blockId == Block.BEDROCK.id() || blockId == Block.END_PORTAL.id();
    }
}
