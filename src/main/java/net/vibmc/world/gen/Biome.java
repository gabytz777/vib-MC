package net.vibmc.world.gen;

/**
 * Coarse climate classification used to vary terrain and vegetation. Kept intentionally
 * simple: two large-scale noise fields (temperature, moisture) pick one of four biomes,
 * and each biome nudges surface blocks and tree density rather than driving a full
 * terrain rewrite.
 */
public enum Biome {
    PLAINS(1),
    DESERT(2),
    SNOW(12),   // vanilla "Ice Plains"
    FOREST(4);

    private final int protocolId;

    Biome(int protocolId) {
        this.protocolId = protocolId;
    }

    /** Vanilla 1.12.2 biome id, written into the chunk's biome array. */
    public int protocolId() {
        return protocolId;
    }

    public static Biome at(TerrainGenerator terrain, int worldX, int worldZ) {
        double temperature = terrain.fbm(worldX * 0.0015 + 500.0, worldZ * 0.0015 + 500.0, 3);
        double moisture = terrain.fbm(worldX * 0.0015 - 500.0, worldZ * 0.0015 - 500.0, 3);
        if (temperature < -0.35) {
            return SNOW;
        }
        if (temperature > 0.35) {
            return DESERT;
        }
        return moisture > 0.1 ? FOREST : PLAINS;
    }
}
