package net.vibmc.item;

public enum ItemType {
    AIR(0, "air", 64),
    STONE(1, "stone", 64),
    GRASS_BLOCK(2, "grass_block", 64),
    DIRT(3, "dirt", 64),
    PLANKS(5, "planks", 64),
    WATER(9, "water", 64),
    LAVA(11, "lava", 64),
    LOG(17, "log", 64),
    LEAVES(18, "leaves", 64),
    SAND(12, "sand", 64),
    GRAVEL(13, "gravel", 64),
    CHEST(54, "chest", 64),
    FURNACE(61, "furnace", 64),
    CRAFTING_TABLE(58, "crafting_table", 64),
    DOOR(64, "door", 64),
    TRAPDOOR(96, "trapdoor", 64),
    COBBLESTONE(4, "cobblestone", 64),
    BEDROCK(7, "bedrock", 64),
    IRON_ORE(15, "iron_ore", 64),
    COAL_ORE(16, "coal_ore", 64),
    SNOW_BLOCK(80, "snow_block", 64),
    NETHERRACK(87, "netherrack", 64),
    SOUL_SAND(88, "soul_sand", 64),
    GLOWSTONE(89, "glowstone", 64),
    OBSIDIAN(49, "obsidian", 64),
    END_STONE(121, "end_stone", 64),
    QUARTZ_ORE(153, "quartz_ore", 64),
    QUARTZ(406, "quartz", 64),
    END_PORTAL_FRAME(120, "end_portal_frame", 64),
    ENDER_EYE(381, "ender_eye", 64),
    STICK(280, "stick", 64),
    APPLE(260, "apple", 64),
    DIAMOND(264, "diamond", 64),
    COAL(263, "coal", 64),
    // Portal-building tools. There is no crafting yet, so these come from /give. The
    // pickaxe matters as much as the rest: the client decides how long a block takes to
    // break, and bare hands never finish obsidian.
    DIAMOND_PICKAXE(278, "diamond_pickaxe", 1),
    FLINT_AND_STEEL(259, "flint_and_steel", 1),
    BUCKET(325, "bucket", 16),
    WATER_BUCKET(326, "water_bucket", 1),
    LAVA_BUCKET(327, "lava_bucket", 1);

    private final int id;
    private final String name;
    private final int maxStackSize;

    ItemType(int id, String name, int maxStackSize) {
        this.id = id;
        this.name = name;
        this.maxStackSize = maxStackSize;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public static ItemType fromName(String name) {
        if (name == null) return null;
        String normalized = name.trim().toLowerCase().replace(' ', '_');
        for (ItemType type : values()) {
            if (type.name.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    public static ItemType fromId(int id) {
        for (ItemType type : values()) {
            if (type.id == id) return type;
        }
        return AIR;
    }
}
