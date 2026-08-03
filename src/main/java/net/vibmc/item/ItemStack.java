package net.vibmc.item;

public class ItemStack {
    private final ItemType type;
    private int amount;

    public ItemStack(ItemType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public ItemType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    public boolean isEmpty() {
        return type == ItemType.AIR || amount <= 0;
    }

    public boolean isSimilar(ItemStack other) {
        return other != null && type == other.type;
    }

    public ItemStack copy() {
        return new ItemStack(type, amount);
    }
}
