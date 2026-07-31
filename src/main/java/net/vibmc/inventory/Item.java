package net.vibmc.inventory;

public enum Item {
    AIR(0, 64),
    STONE(1, 64),
    DIAMOND(2, 64),
    APPLE(3, 64),
    WOODEN_SWORD(4, 1),
    IRON_SWORD(5, 1),
    LEATHER_HELMET(6, 1),
    LEATHER_CHESTPLATE(7, 1),
    LEATHER_LEGGINGS(8, 1),
    LEATHER_BOOTS(9, 1),
    PICKAXE(10, 1);

    private final int id;
    private final int maxStackSize;

    Item(int id, int maxStackSize) {
        this.id = id;
        this.maxStackSize = maxStackSize;
    }

    public int id() {
        return id;
    }

    public int maxStackSize() {
        return maxStackSize;
    }
}
