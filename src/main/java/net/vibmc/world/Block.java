package net.vibmc.world;

public enum Block {
    AIR((short) 0, 0.0f, 0.0f),
    STONE((short) 1, 1.5f, 6.0f),
    GRASS((short) 2, 1.0f, 3.0f),
    DIRT((short) 3, 1.0f, 3.0f),
    WOOD((short) 4, 1.0f, 2.0f),
    LEAVES((short) 5, 0.2f, 0.5f),
    WATER((short) 6, 0.0f, 0.0f),
    LAVA((short) 7, 0.0f, 0.0f),
    CHEST((short) 8, 1.0f, 2.5f),
    FURNACE((short) 9, 1.0f, 2.5f),
    CRAFTING_TABLE((short) 10, 1.0f, 2.5f),
    DOOR((short) 11, 1.0f, 2.5f),
    TRAPDOOR((short) 12, 1.0f, 2.5f);

    private final short id;
    private final float hardness;
    private final float blastResistance;

    Block(short id, float hardness, float blastResistance) {
        this.id = id;
        this.hardness = hardness;
        this.blastResistance = blastResistance;
    }

    public short id() {
        return id;
    }

    public float hardness() {
        return hardness;
    }

    public float blastResistance() {
        return blastResistance;
    }
}
