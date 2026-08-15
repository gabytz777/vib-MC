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
    TRAPDOOR((short) 12, 1.0f, 2.5f),
    SAND((short) 13, 0.5f, 2.5f),
    GRAVEL((short) 14, 0.6f, 3.0f),
    BEDROCK((short) 15, -1.0f, 3600000.0f),
    ANDESITE((short) 16, 1.5f, 6.0f),
    DIORITE((short) 17, 1.5f, 6.0f),
    COAL_ORE((short) 18, 3.0f, 5.0f),
    IRON_ORE((short) 19, 3.0f, 5.0f),
    SNOW((short) 20, 0.2f, 1.0f),
    DOOR_TOP((short) 21, 1.0f, 2.5f), // upper half of a door; same protocol id as DOOR, different metadata
    PLANKS((short) 22, 2.0f, 3.0f),
    COBBLESTONE((short) 23, 2.0f, 6.0f),
    NETHERRACK((short) 24, 0.4f, 2.0f),
    SOUL_SAND((short) 25, 0.5f, 2.5f),
    GLOWSTONE((short) 26, 0.3f, 1.5f),
    OBSIDIAN((short) 27, 50.0f, 1200.0f),
    NETHER_PORTAL((short) 28, -1.0f, 0.0f),
    END_STONE((short) 29, 3.0f, 45.0f),
    END_PORTAL((short) 30, -1.0f, 3600000.0f),
    END_PORTAL_FRAME((short) 31, -1.0f, 3600000.0f);

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

    public short metadata() {
        switch (this) {
            case ANDESITE:
                return 5; // stone:5
            case DIORITE:
                return 3; // stone:3
            case DOOR_TOP:
                return 8; // door upper-half flag bit
            default:
                return 0;
        }
    }

    /**
     * Maps an internal block id to the vanilla block id used by the protocol.
     * Block states are encoded as (vanilla id << 4 | metadata).
     */
    public static short protocolIdOf(short internalId) {
        for (Block block : values()) {
            if (block.id == internalId) {
                return block.protocolId();
            }
        }
        return 0;
    }

    /**
     * Full protocol state id: (vanilla block id << 4) | metadata.
     */
    public static short stateIdOf(short internalId) {
        for (Block block : values()) {
            if (block.id == internalId) {
                return (short) ((block.protocolId() << 4) | block.metadata());
            }
        }
        return 0;
    }

    public short protocolId() {
        switch (this) {
            case STONE:
                return 1;
            case GRASS:
                return 2;
            case DIRT:
                return 3;
            case WOOD:
                return 17; // log
            case LEAVES:
                return 18;
            case WATER:
                return 9;
            case LAVA:
                return 11;
            case SAND:
                return 12;
            case GRAVEL:
                return 13;
            case BEDROCK:
                return 7;
            case CHEST:
                return 54;
            case FURNACE:
                return 61;
            case CRAFTING_TABLE:
                return 58;
            case DOOR:
            case DOOR_TOP:
                return 64;
            case TRAPDOOR:
                return 96;
            case ANDESITE:
            case DIORITE:
                return 1; // stone block, differentiated by metadata
            case COAL_ORE:
                return 16;
            case IRON_ORE:
                return 15;
            case SNOW:
                return 80; // snow block (full cube, not the thin snow layer)
            case PLANKS:
                return 5; // oak planks
            case COBBLESTONE:
                return 4;
            case NETHERRACK:
                return 87;
            case SOUL_SAND:
                return 88;
            case GLOWSTONE:
                return 89;
            case OBSIDIAN:
                return 49;
            case NETHER_PORTAL:
                return 90;
            case END_STONE:
                return 121;
            case END_PORTAL:
                return 119;
            case END_PORTAL_FRAME:
                return 120;
            default:
                return 0;
        }
    }
}
