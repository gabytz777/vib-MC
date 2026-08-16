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

    /**
     * Where players materialise when they enter the End, and how much void is kept clear
     * around it. Arriving is supposed to mean standing on a small obsidian slab with
     * nothing under you and the island off in the distance - which only reads that way if
     * the generator is told not to grow an island there.
     */
    public static final int PLATFORM_X = 100;
    public static final int PLATFORM_Z = 0;
    public static final int PLATFORM_Y = 50;
    private static final int PLATFORM_CLEARANCE = 20;

    /** Obsidian towers ringing the central island, as the End has. */
    private static final int PILLAR_COUNT = 10;
    private static final int PILLAR_RING_RADIUS = 30;

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

                raisePillar(chunk, x, z, worldX, worldZ);
            }
        }
    }

    /**
     * Builds the part of an obsidian tower that falls in this column, if any.
     *
     * <p>Worked out per column from world coordinates rather than placed per chunk, so a
     * tower straddling a chunk border comes out whole with no agreement needed between the
     * two chunks.
     */
    private void raisePillar(Chunk chunk, int x, int z, int worldX, int worldZ) {
        for (int i = 0; i < PILLAR_COUNT; i++) {
            double angle = i * 2 * Math.PI / PILLAR_COUNT;
            int pillarX = (int) Math.round(Math.cos(angle) * PILLAR_RING_RADIUS);
            int pillarZ = (int) Math.round(Math.sin(angle) * PILLAR_RING_RADIUS);

            int radius = 2 + (i % 3);
            int dx = worldX - pillarX;
            int dz = worldZ - pillarZ;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }

            int height = 18 + (i * 7) % 17;
            int top = ISLAND_SURFACE + height;
            for (int y = ISLAND_SURFACE; y < top; y++) {
                chunk.setBlock(x, y, z, Block.OBSIDIAN.id());
            }
            // Bedrock cap, as the towers in the End are topped.
            chunk.setBlock(x, top, z, Block.BEDROCK.id());
            return;
        }
    }

    /**
     * How thick the island is at this column, or null where there is only void.
     *
     * @param distance distance from the world origin, which the central island is built around
     */
    private Integer islandThickness(TerrainGenerator terrain, int worldX, int worldZ, double distance) {
        // Keep the arrival platform's surroundings empty so it really is a slab in the void.
        double fromPlatform = Math.hypot(worldX - PLATFORM_X, worldZ - PLATFORM_Z);
        if (fromPlatform <= PLATFORM_CLEARANCE) {
            return null;
        }
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
