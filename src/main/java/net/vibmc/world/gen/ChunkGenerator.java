package net.vibmc.world.gen;

import net.vibmc.world.Chunk;
import net.vibmc.world.World;

/**
 * Builds the blocks of a chunk that has never been saved.
 *
 * <p>Generation is deliberately separate from persistence: a generator is only ever asked
 * for chunks that are not on disk, and every implementation must be a pure function of the
 * world seed and the chunk coordinates so the same chunk is identical however and whenever
 * it is produced.
 */
public interface ChunkGenerator {
    /** Fills in a freshly created, all-air chunk. */
    void generate(World world, Chunk chunk, int chunkX, int chunkZ);
}
