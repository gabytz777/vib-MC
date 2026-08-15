package net.vibmc.world;

/**
 * The three worlds a player can be in, and the protocol values that identify them.
 *
 * <p>The dimension id is what goes on the wire in Join Game and Respawn; the client uses
 * it to pick the sky, fog and lighting, so it has to match vanilla exactly.
 */
public enum Dimension {
    /** Normal sky, day/night cycle, sea level 63. */
    OVERWORLD(0, "", 63, true),
    /** Roof at y=128, no sky light, everything lit by the terrain itself. */
    NETHER(-1, "_nether", 32, false),
    /** Floating islands in the void, permanently dim sky light. */
    END(1, "_the_end", 0, false);

    /** Nether coordinates are 1/8 of overworld ones, as in vanilla. */
    public static final int NETHER_SCALE = 8;

    private final int protocolId;
    private final String folderSuffix;
    private final int seaLevel;
    private final boolean hasSkyLight;

    Dimension(int protocolId, String folderSuffix, int seaLevel, boolean hasSkyLight) {
        this.protocolId = protocolId;
        this.folderSuffix = folderSuffix;
        this.seaLevel = seaLevel;
        this.hasSkyLight = hasSkyLight;
    }

    public int protocolId() {
        return protocolId;
    }

    /** Suffix appended to the level name: {@code world}, {@code world_nether}, {@code world_the_end}. */
    public String folderSuffix() {
        return folderSuffix;
    }

    public int seaLevel() {
        return seaLevel;
    }

    /**
     * Whether the client should receive a sky-light array for this dimension's chunks.
     * The Nether and End have none, and sending one anyway makes the client render them
     * as if the sun were shining underground.
     */
    public boolean hasSkyLight() {
        return hasSkyLight;
    }

    /** The biome id written into chunk data for this dimension by default. */
    public int defaultBiomeId() {
        switch (this) {
            case NETHER:
                return 8;  // Hell
            case END:
                return 9;  // The End
            default:
                return 1;  // Plains
        }
    }

    public static Dimension byProtocolId(int id) {
        for (Dimension dimension : values()) {
            if (dimension.protocolId == id) {
                return dimension;
            }
        }
        return OVERWORLD;
    }
}
