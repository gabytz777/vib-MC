package net.vibmc.item;

import java.util.HashMap;
import java.util.Map;

public class ItemType {
    private static final Map<Integer, ItemType> BY_ID = new HashMap<>();
    private static final Map<String, ItemType> BY_NAME = new HashMap<>();

    public static final ItemType AIR = register(0, "minecraft:air", 0, false);
    public static final ItemType STONE = register(1, "minecraft:stone", 64, false);
    public static final ItemType GRASS_BLOCK = register(2, "minecraft:grass_block", 64, false);
    public static final ItemType DIRT = register(3, "minecraft:dirt", 64, false);
    public static final ItemType COBBLESTONE = register(4, "minecraft:cobblestone", 64, false);
    public static final ItemType OAK_PLANKS = register(5, "minecraft:oak_planks", 64, false);
    public static final ItemType BEDROCK = register(7, "minecraft:bedrock", 64, false);
    public static final ItemType SAND = register(12, "minecraft:sand", 64, false);
    public static final ItemType GRAVEL = register(13, "minecraft:gravel", 64, false);
    public static final ItemType IRON_ORE = register(15, "minecraft:iron_ore", 64, false);
    public static final ItemType COAL_ORE = register(16, "minecraft:coal_ore", 64, false);
    public static final ItemType OAK_LOG = register(17, "minecraft:oak_log", 64, false);
    public static final ItemType OAK_LEAVES = register(18, "minecraft:oak_leaves", 64, false);
    public static final ItemType GLASS = register(20, "minecraft:glass", 64, false);
    public static final ItemType DIAMOND = register(264, "minecraft:diamond", 64, false);
    public static final ItemType IRON_INGOT = register(265, "minecraft:iron_ingot", 64, false);
    public static final ItemType GOLD_INGOT = register(266, "minecraft:gold_ingot", 64, false);
    public static final ItemType STICK = register(280, "minecraft:stick", 64, false);
    public static final ItemType BOWL = register(281, "minecraft:bowl", 64, false);
    public static final ItemType STRING = register(287, "minecraft:string", 64, false);
    public static final ItemType FEATHER = register(288, "minecraft:feather", 64, false);
    public static final ItemType GUNPOWDER = register(289, "minecraft:gunpowder", 64, false);
    public static final ItemType WHEAT = register(296, "minecraft:wheat", 64, false);
    public static final ItemType BREAD = register(297, "minecraft:bread", 64, true);
    public static final ItemType LEATHER = register(334, "minecraft:leather", 64, false);
    public static final ItemType BRICK = register(336, "minecraft:brick", 64, false);
    public static final ItemType CLAY = register(337, "minecraft:clay_ball", 64, false);
    public static final ItemType PAPER = register(339, "minecraft:paper", 64, false);
    public static final ItemType BOOK = register(340, "minecraft:book", 64, false);
    public static final ItemType BONE = register(352, "minecraft:bone", 64, false);
    public static final ItemType SUGAR = register(353, "minecraft:sugar", 64, false);
    public static final ItemType CAKE = register(354, "minecraft:cake", 1, true);
    public static final ItemType APPLE = register(260, "minecraft:apple", 64, true);
    public static final ItemType BOW = register(261, "minecraft:bow", 1, false);
    public static final ItemType ARROW = register(262, "minecraft:arrow", 64, false);
    public static final ItemType COAL = register(263, "minecraft:coal", 64, false);
    public static final ItemType ROTTEN_FLESH = register(367, "minecraft:rotten_flesh", 64, true);
    public static final ItemType WOODEN_SWORD = register(268, "minecraft:wooden_sword", 1, false);
    public static final ItemType WOODEN_PICKAXE = register(270, "minecraft:wooden_pickaxe", 1, false);
    public static final ItemType WOODEN_AXE = register(271, "minecraft:wooden_axe", 1, false);
    public static final ItemType STONE_SWORD = register(272, "minecraft:stone_sword", 1, false);
    public static final ItemType STONE_PICKAXE = register(274, "minecraft:stone_pickaxe", 1, false);
    public static final ItemType STONE_AXE = register(275, "minecraft:stone_axe", 1, false);
    public static final ItemType IRON_SWORD = register(267, "minecraft:iron_sword", 1, false);
    public static final ItemType IRON_PICKAXE = register(257, "minecraft:iron_pickaxe", 1, false);
    public static final ItemType IRON_AXE = register(258, "minecraft:iron_axe", 1, false);
    public static final ItemType DIAMOND_SWORD = register(276, "minecraft:diamond_sword", 1, false);
    public static final ItemType DIAMOND_PICKAXE = register(278, "minecraft:diamond_pickaxe", 1, false);
    public static final ItemType DIAMOND_AXE = register(279, "minecraft:diamond_axe", 1, false);
    public static final ItemType LEATHER_HELMET = register(298, "minecraft:leather_helmet", 1, false);
    public static final ItemType LEATHER_CHESTPLATE = register(299, "minecraft:leather_chestplate", 1, false);
    public static final ItemType LEATHER_LEGGINGS = register(300, "minecraft:leather_leggings", 1, false);
    public static final ItemType LEATHER_BOOTS = register(301, "minecraft:leather_boots", 1, false);
    public static final ItemType IRON_HELMET = register(306, "minecraft:iron_helmet", 1, false);
    public static final ItemType IRON_CHESTPLATE = register(307, "minecraft:iron_chestplate", 1, false);
    public static final ItemType IRON_LEGGINGS = register(308, "minecraft:iron_leggings", 1, false);
    public static final ItemType IRON_BOOTS = register(309, "minecraft:iron_boots", 1, false);
    public static final ItemType DIAMOND_HELMET = register(310, "minecraft:diamond_helmet", 1, false);
    public static final ItemType DIAMOND_CHESTPLATE = register(311, "minecraft:diamond_chestplate", 1, false);
    public static final ItemType DIAMOND_LEGGINGS = register(312, "minecraft:diamond_leggings", 1, false);
    public static final ItemType DIAMOND_BOOTS = register(313, "minecraft:diamond_boots", 1, false);
    public static final ItemType BEEF = register(363, "minecraft:beef", 64, true);
    public static final ItemType COOKED_BEEF = register(364, "minecraft:cooked_beef", 64, true);
    public static final ItemType PORKCHOP = register(319, "minecraft:porkchop", 64, true);
    public static final ItemType COOKED_PORKCHOP = register(320, "minecraft:cooked_porkchop", 64, true);
    public static final ItemType MUTTON = register(423, "minecraft:mutton", 64, true);
    public static final ItemType COOKED_MUTTON = register(424, "minecraft:cooked_mutton", 64, true);
    public static final ItemType CHICKEN = register(365, "minecraft:chicken", 64, true);
    public static final ItemType COOKED_CHICKEN = register(366, "minecraft:cooked_chicken", 64, true);
    public static final ItemType OAK_DOOR = register(427, "minecraft:oak_door", 64, false);
    public static final ItemType IRON_DOOR = register(430, "minecraft:iron_door", 64, false);
    public static final ItemType OAK_TRAPDOOR = register(428, "minecraft:oak_trapdoor", 64, false);
    public static final ItemType CHEST = register(54, "minecraft:chest", 64, false);
    public static final ItemType FURNACE = register(61, "minecraft:furnace", 64, false);
    public static final ItemType CRAFTING_TABLE = register(58, "minecraft:crafting_table", 64, false);
    public static final ItemType SNOWBALL = register(332, "minecraft:snowball", 16, false);
    public static final ItemType EGG = register(344, "minecraft:egg", 16, false);
    public static final ItemType COMPASS = register(345, "minecraft:compass", 1, false);
    public static final ItemType CLOCK = register(347, "minecraft:clock", 1, false);
    public static final ItemType NAME_TAG = register(421, "minecraft:name_tag", 64, false);

    private final int id;
    private final String name;
    private final int maxStackSize;
    private final boolean food;

    private ItemType(int id, String name, int maxStackSize, boolean food) {
        this.id = id;
        this.name = name;
        this.maxStackSize = maxStackSize;
        this.food = food;
    }

    private static ItemType register(int id, String name, int maxStackSize, boolean food) {
        ItemType type = new ItemType(id, name, maxStackSize, food);
        BY_ID.put(id, type);
        BY_NAME.put(name, type);
        return type;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getMaxStackSize() { return maxStackSize; }
    public boolean isFood() { return food; }

    public int getFoodRestoration() {
        return switch (id) {
            case 260 -> 4;  // apple
            case 297 -> 5;  // bread
            case 319 -> 3;  // porkchop
            case 320 -> 8;  // cooked porkchop
            case 354 -> 14; // cake (per slice)
            case 363 -> 3;  // beef
            case 364 -> 8;  // cooked beef
            case 365 -> 2;  // chicken
            case 366 -> 6;  // cooked chicken
            case 367 -> 4;  // rotten flesh
            case 423 -> 2;  // mutton
            case 424 -> 6;  // cooked mutton
            default -> 0;
        };
    }

    public static ItemType getById(int id) {
        return BY_ID.getOrDefault(id, AIR);
    }

    public static ItemType getByName(String name) {
        return BY_NAME.getOrDefault(name, AIR);
    }
}
