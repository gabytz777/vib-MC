package net.vibmc.inventory.container;

import net.vibmc.inventory.Inventory;

public class ChestInventory extends Inventory {
    public ChestInventory() {
        super("Chest", 27);
    }

    public ChestInventory(int size) {
        super("Chest", size);
    }

    public ChestInventory(String title, int size) {
        super(title, size);
    }
}
