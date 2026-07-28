package net.vibmc;

import net.vibmc.world.block.BlockType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BlockTypeTest {

    @Test
    public void testBlockTypeRegistry() {
        assertEquals(BlockType.AIR, BlockType.getById(0));
        assertEquals(BlockType.STONE, BlockType.getById(1));
        assertEquals(BlockType.GRASS, BlockType.getById(2));
        assertEquals(BlockType.DIRT, BlockType.getById(3));
    }

    @Test
    public void testBlockTypeByName() {
        assertEquals(BlockType.STONE, BlockType.getByName("minecraft:stone"));
        assertEquals(BlockType.AIR, BlockType.getByName("minecraft:air"));
        assertNotNull(BlockType.getByName("minecraft:diamond_ore"));
    }

    @Test
    public void testAirDetection() {
        assertTrue(BlockType.AIR.isAir());
        assertFalse(BlockType.STONE.isAir());
    }

    @Test
    public void testProperties() {
        assertEquals(-1.0f, BlockType.BEDROCK.getHardness(), 0.0f);
        assertEquals(3600000.0f, BlockType.BEDROCK.getBlastResistance(), 0.0f);
        assertTrue(BlockType.STONE.isSolid());
        assertFalse(BlockType.AIR.isSolid());
    }
}
