package net.vibmc.world;

import net.vibmc.world.storage.WorldStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkPersistenceTest {
    private static final long SEED = 987654321L;

    private World worldIn(Path dir) {
        String name = dir.resolve("world").toString();
        return new World(SEED, name, new WorldStorage(name));
    }

    @Test
    void generatedChunkIsDirtyAndSavesOnce(@TempDir Path dir) {
        World world = worldIn(dir);
        Chunk chunk = world.getChunk(0, 0);

        assertTrue(chunk.isDirty(), "a freshly generated chunk has never been written");
        assertEquals(1, world.chunkManager().saveAll());
        assertFalse(chunk.isDirty());
        assertEquals(0, world.chunkManager().saveAll(), "an unchanged chunk is not rewritten");
    }

    @Test
    void placedBlocksSurviveAReload(@TempDir Path dir) {
        World first = worldIn(dir);
        Chunk chunk = first.getChunk(3, -2);
        short generated = chunk.getBlock(9, 0, 9);
        chunk.setBlock(5, 40, 7, Block.CHEST.id());
        first.chunkManager().saveAll();

        World reloaded = worldIn(dir);
        Chunk restored = reloaded.getChunk(3, -2);

        assertEquals(Block.CHEST.id(), restored.getBlock(5, 40, 7));
        assertEquals(generated, restored.getBlock(9, 0, 9), "generated terrain is preserved too");
        assertFalse(restored.isDirty(), "a chunk read from disk starts clean");
    }

    @Test
    void chunkNotOnDiskIsStillGenerated(@TempDir Path dir) {
        World world = worldIn(dir);
        Chunk chunk = world.getChunk(64, 64);

        assertEquals(Block.BEDROCK.id(), chunk.getBlock(0, 0, 0));
        assertTrue(chunk.isDirty());
    }

    @Test
    void unreadableChunkFallsBackToGeneration(@TempDir Path dir) throws IOException {
        World world = worldIn(dir);
        world.getChunk(1, 1);
        world.chunkManager().saveAll();

        Path region = dir.resolve("world").resolve("region").resolve("r.1.1.chunk");
        java.nio.file.Files.write(region, new byte[]{1, 2, 3});

        World reloaded = worldIn(dir);
        Chunk chunk = reloaded.getChunk(1, 1);

        assertEquals(Block.BEDROCK.id(), chunk.getBlock(0, 0, 0),
                "a damaged chunk regenerates instead of taking the server down");
    }

    @Test
    void setBlockOnlyMarksDirtyWhenSomethingChanges(@TempDir Path dir) {
        World world = worldIn(dir);
        Chunk chunk = world.getChunk(0, 0);
        world.chunkManager().saveAll();
        assertFalse(chunk.isDirty());

        short existing = chunk.getBlock(4, 0, 4);
        chunk.setBlock(4, 0, 4, existing);
        assertFalse(chunk.isDirty(), "rewriting the same block is not a change");

        assertNotEquals(Block.LAVA.id(), existing);
        chunk.setBlock(4, 0, 4, Block.LAVA.id());
        assertTrue(chunk.isDirty());
    }

    @Test
    void unsavedChunkCountTracksPendingWork(@TempDir Path dir) {
        World world = worldIn(dir);
        world.getChunk(0, 0);
        world.getChunk(0, 1);

        assertEquals(2, world.chunkManager().getUnsavedChunkCount());
        world.chunkManager().saveAll();
        assertEquals(0, world.chunkManager().getUnsavedChunkCount());
    }
}
