package net.vibmc.inventory;

import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;

import java.util.Arrays;

public class Inventory {
    protected final ItemStack[] slots;
    protected final String title;
    protected final int size;

    public Inventory(String title, int size) {
        this.title = title;
        this.size = size;
        this.slots = new ItemStack[size];
        fillAir();
    }

    private void fillAir() {
        for (int i = 0; i < size; i++) {
            slots[i] = new ItemStack(ItemType.AIR, 0);
        }
    }

    public ItemStack getSlot(int index) {
        if (index < 0 || index >= size) return new ItemStack(ItemType.AIR, 0);
        return slots[index];
    }

    public void setSlot(int index, ItemStack item) {
        if (index < 0 || index >= size) return;
        slots[index] = item != null ? item : new ItemStack(ItemType.AIR, 0);
    }

    public int addItem(ItemStack item) {
        if (item == null || item.isEmpty()) return 0;

        int remaining = item.getAmount();

        // Try to stack with existing items
        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack existing = slots[i];
            if (existing != null && !existing.isEmpty() && existing.isSimilar(item)) {
                int space = existing.getType().getMaxStackSize() - existing.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(remaining, space);
                    existing.setAmount(existing.getAmount() + toAdd);
                    remaining -= toAdd;
                }
            }
        }

        // Try empty slots
        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack existing = slots[i];
            if (existing == null || existing.isEmpty()) {
                int toAdd = Math.min(remaining, item.getType().getMaxStackSize());
                slots[i] = new ItemStack(item.getType(), toAdd);
                remaining -= toAdd;
            }
        }

        return remaining;
    }

    public void removeItem(int index, int amount) {
        if (index < 0 || index >= size) return;
        ItemStack slot = slots[index];
        if (slot == null || slot.isEmpty()) return;
        slot.setAmount(slot.getAmount() - amount);
        if (slot.getAmount() <= 0) {
            slots[index] = new ItemStack(ItemType.AIR, 0);
        }
    }

    public boolean hasItem(ItemType type, int amount) {
        int count = 0;
        for (ItemStack slot : slots) {
            if (slot != null && slot.getType() == type) {
                count += slot.getAmount();
            }
        }
        return count >= amount;
    }

    public int countItem(ItemType type) {
        int count = 0;
        for (ItemStack slot : slots) {
            if (slot != null && slot.getType() == type) {
                count += slot.getAmount();
            }
        }
        return count;
    }

    public void clear() {
        fillAir();
    }

    public String getTitle() { return title; }
    public int getSize() { return size; }
    public ItemStack[] getSlots() { return slots; }
}
