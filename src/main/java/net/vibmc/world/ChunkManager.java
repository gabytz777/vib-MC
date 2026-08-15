package net.vibmc.world;

import net.vibmc.server.VibMC;
import net.vibmc.server.util.Logger;
import net.vibmc.world.storage.WorldStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChunkManager {
    private final World world;
    private final WorldStorage storage;
    private final Map<Long, Chunk> loadedChunks = new LinkedHashMap<>();

    public ChunkManager(World world) {
        this(world, world.storage());
    }

    public ChunkManager(World world, WorldStorage storage) {
        this.world = world;
        this.storage = storage;
    }

    /**
     * Returns the chunk at the given coordinates, loading it from disk if it has been
     * saved before and generating fresh terrain only when it has not.
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        synchronized (loadedChunks) {
            Chunk chunk = loadedChunks.get(key);
            if (chunk == null) {
                chunk = loadOrGenerate(chunkX, chunkZ);
                loadedChunks.put(key, chunk);
            }
            return chunk;
        }
    }

    private Chunk loadOrGenerate(int chunkX, int chunkZ) {
        try {
            short[] stored = storage.readChunk(chunkX, chunkZ);
            if (stored != null) {
                return Chunk.fromStored(world, chunkX, chunkZ, stored);
            }
        } catch (IOException e) {
            warn("Could not read chunk %d,%d (%s); regenerating it", chunkX, chunkZ, e.getMessage());
        }
        return Chunk.generate(world, chunkX, chunkZ);
    }

    public List<Chunk> listLoadedChunks() {
        synchronized (loadedChunks) {
            return new ArrayList<>(loadedChunks.values());
        }
    }

    public int getLoadedChunkCount() {
        synchronized (loadedChunks) {
            return loadedChunks.size();
        }
    }

    /**
     * Writes every loaded chunk that has unsaved changes.
     *
     * @return the number of chunks written
     */
    public int saveAll() {
        int written = 0;
        for (Chunk chunk : listLoadedChunks()) {
            if (!chunk.isDirty()) {
                continue;
            }
            try {
                storage.writeChunk(chunk.chunkX(), chunk.chunkZ(), chunk.blocks());
                chunk.markSaved();
                written++;
            } catch (IOException e) {
                warn("Failed to save chunk %d,%d: %s", chunk.chunkX(), chunk.chunkZ(), e);
            }
        }
        return written;
    }

    /** Number of loaded chunks with changes that are not on disk yet. */
    public int getUnsavedChunkCount() {
        int dirty = 0;
        for (Chunk chunk : listLoadedChunks()) {
            if (chunk.isDirty()) {
                dirty++;
            }
        }
        return dirty;
    }

    private static long key(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static void warn(String message, Object... args) {
        VibMC server = VibMC.getInstance();
        if (server == null) {
            return;
        }
        Logger logger = server.getLogger();
        if (logger != null) {
            logger.warn(message, args);
        }
    }
}
