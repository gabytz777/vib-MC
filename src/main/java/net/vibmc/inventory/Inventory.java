package net.vibmc.inventory;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final List<ItemStack> slots;

    public Inventory(int size) {
        this.slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(new ItemStack(Item.AIR, 0));
        }
    }

    public void addItem(ItemStack itemStack) {
        for (int i = 0; i < slots.size(); i++) {
            ItemStack slot = slots.get(i);
            if (slot.item() == Item.AIR) {
                slots.set(i, itemStack);
                return;
            }
        }
    }

    public List<ItemStack> slots() {
        return List.copyOf(slots);
    }
}
