package net.vibmc;

import net.vibmc.inventory.Inventory;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryTest {

    @Test
    public void testAddItem() {
        Inventory inv = new Inventory("Test", 27);
        int remaining = inv.addItem(new ItemStack(ItemType.STONE, 30));
        assertEquals(0, remaining);
        assertEquals(30, inv.countItem(ItemType.STONE));
    }

    @Test
    public void testAddItemOverflow() {
        Inventory inv = new Inventory("Test", 1);
        inv.addItem(new ItemStack(ItemType.STONE, 64));
        int remaining = inv.addItem(new ItemStack(ItemType.COBBLESTONE, 64));
        assertEquals(64, remaining);
    }

    @Test
    public void testHasItem() {
        Inventory inv = new Inventory("Test", 27);
        inv.addItem(new ItemStack(ItemType.DIAMOND, 5));
        assertTrue(inv.hasItem(ItemType.DIAMOND, 5));
        assertFalse(inv.hasItem(ItemType.DIAMOND, 6));
    }

    @Test
    public void testRemoveItem() {
        Inventory inv = new Inventory("Test", 27);
        inv.addItem(new ItemStack(ItemType.DIRT, 10));
        inv.removeItem(0, 3);
        assertEquals(7, inv.countItem(ItemType.DIRT));
    }

    @Test
    public void testClear() {
        Inventory inv = new Inventory("Test", 27);
        inv.addItem(new ItemStack(ItemType.STONE, 10));
        assertFalse(inv.getSlot(0).isEmpty());
        inv.clear();
        assertTrue(inv.getSlot(0).isEmpty());
    }

    @Test
    public void testMultipleItemTypes() {
        Inventory inv = new Inventory("Test", 27);
        inv.addItem(new ItemStack(ItemType.STONE, 10));
        inv.addItem(new ItemStack(ItemType.DIRT, 5));
        assertEquals(10, inv.countItem(ItemType.STONE));
        assertEquals(5, inv.countItem(ItemType.DIRT));
    }
}
