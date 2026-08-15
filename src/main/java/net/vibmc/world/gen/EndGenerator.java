package net.vibmc.world.gen;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;
import net.vibmc.world.World;

/**
 * The End: end-stone islands floating in the void.
 *
 * <p>The central island is deterministic and always solid, because that is where players
 * arrive and they would otherwise fall straight into the void. Beyond it, islands thin out
 * with distance from the origin. No dragon and no structures, by design.
 */
public class EndGenerator implements ChunkGenerator {
    /** Height the main island's surface sits at. */
    public static final int ISLAND_SURFACE = 64;
    /** Radius, in blocks, of the guaranteed-solid central island. */
    private static final int CENTRAL_RADIUS = 40;

    @Override
    public void generate(World world, Chunk chunk, int chunkX, int chunkZ) {
        TerrainGenerator terrain = new TerrainGenerator(world.seed() ^ 0x54484520454E4421L);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                double distance = Math.sqrt((double) worldX * worldX + (double) worldZ * worldZ);

                Integer thickness = islandThickness(terrain, worldX, worldZ, distance);
                if (thickness == null) {
                    continue; // void
                }

                int top = ISLAND_SURFACE;
                for (int y = top - thickness; y <= top; y++) {
                    if (y >= 0 && y < 256) {
                        chunk.setBlock(x, y, z, Block.END_STONE.id());
                    }
                }
            }
        }
    }

    /**
     * How thick the island is at this column, or null where there is only void.
     *
     * @param distance distance from the world origin, which the central island is built around
     */
    private Integer islandThickness(TerrainGenerator terrain, int worldX, int worldZ, double distance) {
        if (distance <= CENTRAL_RADIUS) {
            // Dome the central island so it is thickest in the middle and tapers at the rim.
            double falloff = 1.0 - (distance / CENTRAL_RADIUS);
            return 4 + (int) (falloff * 12);
        }

        // Outer islands: noise decides where they are, and they get rarer further out.
        double island = terrain.fbm(worldX * 0.008, worldZ * 0.008, 3);
        double required = 0.25 + Math.min(0.35, (distance - CENTRAL_RADIUS) / 4000.0);
        if (island <= required) {
            return null;
        }
        return 3 + (int) ((island - required) * 20);
    }
}
