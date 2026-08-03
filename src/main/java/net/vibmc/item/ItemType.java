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
    STICK(280, "stick", 64),
    APPLE(260, "apple", 64),
    DIAMOND(264, "diamond", 64);

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
