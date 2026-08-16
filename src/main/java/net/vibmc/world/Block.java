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
    END_PORTAL_FRAME((short) 31, -1.0f, 3600000.0f),
    /**
     * A portal surface in a frame that runs north-south. The axis is part of the block
     * state on the wire, so the two orientations have to be separate blocks here - a
     * portal with no axis is not a state the client knows, and it renders as air.
     */
    NETHER_PORTAL_Z((short) 32, -1.0f, 0.0f),
    QUARTZ_ORE((short) 33, 3.0f, 3.0f),
    // An end portal frame is one block with two things in its state: which way it points,
    // and whether an eye of ender is sitting in it. Both are metadata on the wire, so each
    // combination is its own block here.
    END_PORTAL_FRAME_W((short) 34, -1.0f, 3600000.0f),
    END_PORTAL_FRAME_N((short) 35, -1.0f, 3600000.0f),
    END_PORTAL_FRAME_E((short) 36, -1.0f, 3600000.0f),
    END_PORTAL_FRAME_EYE((short) 37, -1.0f, 3600000.0f),
    END_PORTAL_FRAME_W_EYE((short) 38, -1.0f, 3600000.0f),
    END_PORTAL_FRAME_N_EYE((short) 39, -1.0f, 3600000.0f),
    END_PORTAL_FRAME_E_EYE((short) 40, -1.0f, 3600000.0f);

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
            case NETHER_PORTAL:
                return 1; // axis X - the frame runs east-west
            case NETHER_PORTAL_Z:
                return 2; // axis Z - the frame runs north-south
            // Frame metadata is the horizontal facing (south 0, west 1, north 2, east 3)
            // plus 4 when an eye of ender is in it.
            case END_PORTAL_FRAME_W:
                return 1;
            case END_PORTAL_FRAME_N:
                return 2;
            case END_PORTAL_FRAME_E:
                return 3;
            case END_PORTAL_FRAME_EYE:
                return 4;
            case END_PORTAL_FRAME_W_EYE:
                return 5;
            case END_PORTAL_FRAME_N_EYE:
                return 6;
            case END_PORTAL_FRAME_E_EYE:
                return 7;
            default:
                return 0;
        }
    }

    /** The four facings of an empty end portal frame, indexed south, west, north, east. */
    private static final Block[] FRAMES = {
            END_PORTAL_FRAME, END_PORTAL_FRAME_W, END_PORTAL_FRAME_N, END_PORTAL_FRAME_E
    };
    /** The same four facings, with an eye of ender in them. */
    private static final Block[] FRAMES_WITH_EYE = {
            END_PORTAL_FRAME_EYE, END_PORTAL_FRAME_W_EYE,
            END_PORTAL_FRAME_N_EYE, END_PORTAL_FRAME_E_EYE
    };

    /** An empty frame pointing the given way (0 south, 1 west, 2 north, 3 east). */
    public static Block frameFacing(int facing) {
        return FRAMES[Math.floorMod(facing, 4)];
    }

    public static boolean isEndPortalFrame(short internalId) {
        for (int i = 0; i < FRAMES.length; i++) {
            if (FRAMES[i].id == internalId || FRAMES_WITH_EYE[i].id == internalId) {
                return true;
            }
        }
        return false;
    }

    public static boolean frameHasEye(short internalId) {
        for (Block frame : FRAMES_WITH_EYE) {
            if (frame.id == internalId) {
                return true;
            }
        }
        return false;
    }

    /**
     * The same frame with an eye of ender in it, keeping the way it points.
     *
     * @return the eyed frame, or the block unchanged if it was not an empty frame
     */
    public static short frameWithEye(short internalId) {
        for (int i = 0; i < FRAMES.length; i++) {
            if (FRAMES[i].id == internalId) {
                return FRAMES_WITH_EYE[i].id;
            }
        }
        return internalId;
    }

    /** The block with this internal id, or {@link #AIR} if nothing uses it. */
    public static Block byId(short internalId) {
        for (Block block : values()) {
            if (block.id == internalId) {
                return block;
            }
        }
        return AIR;
    }

    /** True for either orientation of a nether portal surface. */
    public static boolean isNetherPortal(short internalId) {
        return internalId == NETHER_PORTAL.id || internalId == NETHER_PORTAL_Z.id;
    }

    /** Blocks a player can walk into: air, portals and liquids. */
    public static boolean isPassable(short internalId) {
        return internalId == AIR.id
                || internalId == WATER.id
                || internalId == LAVA.id
                || isNetherPortal(internalId)
                || internalId == END_PORTAL.id;
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
            case NETHER_PORTAL_Z:
                return 90;
            case END_STONE:
                return 121;
            case END_PORTAL:
                return 119;
            case END_PORTAL_FRAME:
            case END_PORTAL_FRAME_W:
            case END_PORTAL_FRAME_N:
            case END_PORTAL_FRAME_E:
            case END_PORTAL_FRAME_EYE:
            case END_PORTAL_FRAME_W_EYE:
            case END_PORTAL_FRAME_N_EYE:
            case END_PORTAL_FRAME_E_EYE:
                return 120;
            case QUARTZ_ORE:
                return 153;
            default:
                return 0;
        }
    }
}
