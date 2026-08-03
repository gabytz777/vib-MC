package net.vibmc.world;

import java.io.File;
import java.io.IOException;
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

    public List<Chunk> listLoadedChunks() {
        return new ArrayList<>(loadedChunks.values());
    }

    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    public void saveAll() {
        // Persistence is not implemented yet; the method exists for the /save-all command.
    }

    public void saveAll(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            return;
        }
        for (Chunk chunk : listLoadedChunks()) {
            try {
                File file = new File(directory, chunk.chunkX() + "_" + chunk.chunkZ() + ".chunk");
                // Placeholder: chunk data persistence is a future feature.
                file.createNewFile();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
