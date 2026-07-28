package net.vibmc.inventory.container;

import net.vibmc.inventory.Inventory;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;

public class CraftingInventory extends Inventory {
    public CraftingInventory() {
        super("Crafting", 10); // 4 crafting grid + 1 result
    }

    public ItemStack getCraftingSlot(int index) {
        return getSlot(index);
    }

    public ItemStack getResultSlot() {
        return getSlot(9);
    }

    public void setCraftingSlot(int index, ItemStack item) {
        setSlot(index, item);
    }

    public void setResultSlot(ItemStack item) {
        setSlot(9, item);
    }

    public void updateResult() {
        // Simple crafting recipes
        ItemStack result = calculateRecipe();
        setResultSlot(result);
    }

    private ItemStack calculateRecipe() {
        // Check for simple recipes
        // Wooden planks from logs
        if (matchesSingle(ItemType.OAK_LOG)) {
            return new ItemStack(ItemType.OAK_PLANKS, 4);
        }
        // Sticks from planks
        if (matchesTwoVertical(ItemType.OAK_PLANKS)) {
            return new ItemStack(ItemType.STICK, 4);
        }
        // Crafting table
        if (matchesTwoByTwo(ItemType.OAK_PLANKS)) {
            return new ItemStack(ItemType.CRAFTING_TABLE, 1);
        }
        // Wooden sword
        if (matchesWoodenSword()) {
            return new ItemStack(ItemType.WOODEN_SWORD, 1);
        }
        // Wooden pickaxe
        if (matchesWoodenPickaxe()) {
            return new ItemStack(ItemType.WOODEN_PICKAXE, 1);
        }
        // Wooden axe
        if (matchesWoodenAxe()) {
            return new ItemStack(ItemType.WOODEN_AXE, 1);
        }
        return null;
    }

    private boolean matchesSingle(ItemType type) {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack slot = getSlot(i);
            if (slot != null && !slot.isEmpty()) {
                if (slot.getType() == type && slot.getAmount() >= 1) {
                    count++;
                } else {
                    return false;
                }
            }
        }
        return count == 1;
    }

    private boolean matchesTwoVertical(ItemType type) {
        // Two items stacked vertically in grid positions 0 and 1
        ItemStack s0 = getSlot(0);
        ItemStack s1 = getSlot(1);
        if (s0 == null || s0.getType() != type) return false;
        if (s1 == null || s1.getType() != type) return false;
        for (int i = 2; i < 4; i++) {
            ItemStack slot = getSlot(i);
            if (slot != null && !slot.isEmpty()) return false;
        }
        return true;
    }

    private boolean matchesTwoByTwo(ItemType type) {
        for (int i = 0; i < 4; i++) {
            ItemStack slot = getSlot(i);
            if (slot == null || slot.getType() != type) return false;
        }
        return true;
    }

    private boolean matchesWoodenSword() {
        // Stick in bottom, 2 planks above
        ItemStack s0 = getSlot(0);
        ItemStack s1 = getSlot(1);
        ItemStack s2 = getSlot(2);
        if (s0 == null || s0.getType() != ItemType.OAK_PLANKS) return false;
        if (s1 == null || s1.getType() != ItemType.OAK_PLANKS) return false;
        if (s2 == null || s2.getType() != ItemType.STICK) return false;
        ItemStack s3 = getSlot(3);
        if (s3 != null && !s3.isEmpty()) return false;
        return true;
    }

    private boolean matchesWoodenPickaxe() {
        // 3 planks across top, 2 sticks down middle
        ItemStack s0 = getSlot(0);
        ItemStack s1 = getSlot(1);
        ItemStack s2 = getSlot(2);
        ItemStack s3 = getSlot(3);
        if (s0 == null || s0.getType() != ItemType.OAK_PLANKS) return false;
        if (s1 == null || s1.getType() != ItemType.OAK_PLANKS) return false;
        if (s2 == null || s2.getType() != ItemType.OAK_PLANKS) return false;
        if (s3 == null || s3.getType() != ItemType.STICK) return false;
        return true;
    }

    private boolean matchesWoodenAxe() {
        // 2 planks, stick on side
        ItemStack s0 = getSlot(0);
        ItemStack s1 = getSlot(1);
        ItemStack s2 = getSlot(2);
        ItemStack s3 = getSlot(3);
        if (s0 == null || s0.getType() != ItemType.OAK_PLANKS) return false;
        if (s1 == null || s1.getType() != ItemType.OAK_PLANKS) return false;
        if (s2 == null || s2.getType() != ItemType.STICK) return false;
        if (s3 != null && !s3.isEmpty()) return false;
        return true;
    }
}
