package net.vibmc;

import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemStackTest {

    @Test
    public void testCreateItemStack() {
        ItemStack stack = new ItemStack(ItemType.STONE, 10);
        assertEquals(ItemType.STONE, stack.getType());
        assertEquals(10, stack.getAmount());
    }

    @Test
    public void testMaxStackSize() {
        ItemStack stack = new ItemStack(ItemType.STONE, 100);
        assertEquals(64, stack.getAmount());
    }

    @Test
    public void testItemStackCopy() {
        ItemStack original = new ItemStack(ItemType.DIAMOND, 5);
        ItemStack copy = original.copy();
        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getAmount(), copy.getAmount());
        assertNotSame(original, copy);
    }

    @Test
    public void testEmptyStack() {
        ItemStack empty = new ItemStack(ItemType.AIR, 0);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testFoodItems() {
        ItemStack apple = new ItemStack(ItemType.APPLE);
        assertTrue(apple.isFood());
        assertEquals(4, apple.getFoodRestoration());

        ItemStack stone = new ItemStack(ItemType.STONE);
        assertFalse(stone.isFood());
    }

    @Test
    public void testDamageableItems() {
        ItemStack sword = new ItemStack(ItemType.DIAMOND_SWORD);
        assertTrue(sword.isBreakable());
        assertEquals(1561, sword.getMaxDamage());

        sword.setDamage(100);
        assertTrue(sword.isDamaged());
    }
}
