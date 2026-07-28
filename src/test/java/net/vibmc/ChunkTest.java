package net.vibmc;

import net.vibmc.server.util.Position;
import net.vibmc.world.block.Block;
import net.vibmc.world.block.BlockType;
import net.vibmc.world.chunk.Chunk;
import net.vibmc.world.World;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkTest {

    @Test
    public void testChunkCreation() {
        Chunk chunk = new Chunk(0, 0, null);
        assertNotNull(chunk);
        assertEquals(0, chunk.getX());
        assertEquals(0, chunk.getZ());
    }

    @Test
    public void testGetSetBlock() {
        Chunk chunk = new Chunk(0, 0, null);
        Block stone = new Block(BlockType.STONE, 0);
        chunk.setBlock(5, 64, 7, stone);

        Block retrieved = chunk.getBlock(5, 64, 7);
        assertEquals(BlockType.STONE, retrieved.getType());
    }

    @Test
    public void testAirDefault() {
        Chunk chunk = new Chunk(0, 0, null);
        Block retrieved = chunk.getBlock(3, 50, 4);
        assertTrue(retrieved.isAir());
    }

    @Test
    public void testOutOfBounds() {
        Chunk chunk = new Chunk(0, 0, null);
        Block retrieved = chunk.getBlock(20, 50, 30);
        assertTrue(retrieved.isAir());
    }

    @Test
    public void testHighestBlock() {
        Chunk chunk = new Chunk(0, 0, null);
        chunk.setBlock(0, 100, 0, new Block(BlockType.STONE, 0));
        int highest = chunk.getHighestBlockY(0, 0);
        assertEquals(100, highest);
    }

    @Test
    public void testSaveLoadRoundTrip() {
        Chunk chunk = new Chunk(1, 2, null);
        chunk.setBlock(0, 50, 0, new Block(BlockType.STONE, 0));
        chunk.setBlock(15, 100, 15, new Block(BlockType.DIAMOND_ORE, 0));

        byte[] data = chunk.saveToBytes();

        Chunk loaded = new Chunk(1, 2, null);
        loaded.loadFromBytes(data);

        assertEquals(BlockType.STONE, loaded.getBlock(0, 50, 0).getType());
        assertEquals(BlockType.DIAMOND_ORE, loaded.getBlock(15, 100, 15).getType());
        assertTrue(loaded.isLoaded());
    }
}
