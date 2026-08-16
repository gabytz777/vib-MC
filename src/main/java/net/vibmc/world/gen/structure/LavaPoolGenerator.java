package net.vibmc.world.gen.structure;

import net.vibmc.world.Block;
import net.vibmc.world.Chunk;
import net.vibmc.world.gen.TerrainGenerator;

/**
 * Rare surface lava pools sitting in a bed of stone.
 *
 * <p>These are the overworld's supply of obsidian, and therefore the way a player gets
 * into the Nether: nothing is handed out at spawn, so a portal frame has to be quarried
 * out of a pool somebody found. That only works if the pools are worth finding, so they
 * are deliberately uncommon - roughly one chunk in a hundred is even considered - and the
 * stone bed keeps the lava from draining into whatever soil or cave is underneath.
 */
public final class LavaPoolGenerator {
    /** One chunk in this many is considered for a pool. */
    public static final int CHUNKS_PER_POOL = 100;

    /** Outer radius of the stone bed, in blocks. */
    private static final int RADIUS = 3;
    /** How far above sea level the ground has to be before a pool is placed. */
    private static final int MIN_HEIGHT_ABOVE_SEA = 2;

    private LavaPoolGenerator() {
    }

    public static void apply(Chunk chunk, TerrainGenerator terrain) {
        int chunkX = chunk.chunkX();
        int chunkZ = chunk.chunkZ();

        // A hash of the chunk coordinates, not a running random: the same chunk decides the
        // same way however many times it is generated, and neighbours decide independently.
        int roll = terrain.hash(chunkX * 2 + 1, chunkZ * 2 - 1);
        if (roll % CHUNKS_PER_POOL != 0) {
            return;
        }

        // Keep the whole pool inside this chunk, so it never depends on a neighbour that
        // may not be generated yet.
        int localX = RADIUS + 1 + ((roll >> 8) & 7);
        int localZ = RADIUS + 1 + ((roll >> 16) & 7);
        int worldX = chunkX * 16 + localX;
        int worldZ = chunkZ * 16 + localZ;

        int surface = terrain.surfaceHeight(worldX, worldZ);
        if (surface <= TerrainGenerator.SEA_LEVEL + MIN_HEIGHT_ABOVE_SEA) {
            return;  // no pools out at sea or down on the beach
        }
        if (!isFlatEnough(terrain, worldX, worldZ, surface)) {
            return;  // a pool on a hillside would just pour down it
        }

        carve(chunk, localX, localZ, surface);
    }

    /** True when every column the pool covers sits at very nearly the same height. */
    private static boolean isFlatEnough(TerrainGenerator terrain, int worldX, int worldZ, int surface) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                if (Math.abs(terrain.surfaceHeight(worldX + dx, worldZ + dz) - surface) > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Stamps the pool: a stone bed two blocks thick under everything, a stone rim at ground
     * level, and a single layer of lava in the middle with clear air above it.
     */
    private static void carve(Chunk chunk, int localX, int localZ, int surface) {
        int inner = RADIUS - 1;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance > RADIUS * RADIUS) {
                    continue;
                }
                int x = localX + dx;
                int z = localZ + dz;

                // Bed: stone under the whole footprint so the lava has nowhere to drain.
                chunk.setBlock(x, surface - 1, z, Block.STONE.id());
                chunk.setBlock(x, surface - 2, z, Block.STONE.id());

                if (distance <= inner * inner) {
                    chunk.setBlock(x, surface, z, Block.LAVA.id());
                } else {
                    chunk.setBlock(x, surface, z, Block.STONE.id());
                }

                // Clear whatever the surface pass or a tree left standing over the pool.
                for (int dy = 1; dy <= 4; dy++) {
                    chunk.setBlock(x, surface + dy, z, Block.AIR.id());
                }
            }
        }
    }
}
