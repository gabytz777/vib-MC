package net.vibmc.inventory;

public class ItemStack {
    private final Item item;
    private int amount;
    private int durability;

    public ItemStack(Item item, int amount) {
        this.item = item;
        this.amount = Math.max(1, amount);
        this.durability = 100;
    }

    public Item item() {
        return item;
    }

    public int amount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int durability() {
        return durability;
    }

    public void damage(int amount) {
        durability = Math.max(0, durability - amount);
    }
}
