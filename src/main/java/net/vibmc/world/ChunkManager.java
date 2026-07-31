package net.vibmc.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChunkManager {
    private final World world;
    private final Map<Long, Chunk> loadedChunks = new LinkedHashMap<>();

    public ChunkManager(World world) {
        this.world = world;
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
        return loadedChunks.computeIfAbsent(key, ignored -> Chunk.generate(world, chunkX, chunkZ));
    }

    public void tick(long tick) {
        for (Chunk chunk : new ArrayList<>(loadedChunks.values())) {
            chunk.tick(tick);
        }
    }

    public List<Chunk> listLoadedChunks() {
        return new ArrayList<>(loadedChunks.values());
    }
}
