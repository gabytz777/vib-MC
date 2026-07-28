package net.vibmc.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ItemStack {
    private ItemType type;
    private int amount;
    private int damage;
    private int maxDamage;
    private Map<String, Object> nbt;

    public ItemStack(ItemType type) {
        this(type, 1);
    }

    public ItemStack(ItemType type, int amount) {
        this.type = type;
        this.amount = Math.min(amount, type.getMaxStackSize());
        this.damage = 0;
        this.maxDamage = getDefaultMaxDamage(type);
        this.nbt = new HashMap<>();
    }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) {
        this.amount = Math.max(0, Math.min(amount, type.getMaxStackSize()));
    }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public int getMaxDamage() { return maxDamage; }

    public boolean isDamaged() { return damage > 0; }
    public boolean isBreakable() { return maxDamage > 0; }

    public boolean isFood() { return type.isFood(); }
    public int getFoodRestoration() { return type.getFoodRestoration(); }

    public boolean isEmpty() { return type == ItemType.AIR || amount <= 0; }

    public ItemStack copy() {
        ItemStack copy = new ItemStack(type, amount);
        copy.damage = this.damage;
        copy.maxDamage = this.maxDamage;
        copy.nbt = new HashMap<>(this.nbt);
        return copy;
    }

    public boolean isSimilar(ItemStack other) {
        return other != null && this.type == other.type;
    }

    public void addNBT(String key, Object value) {
        nbt.put(key, value);
    }

    public Object getNBT(String key) {
        return nbt.get(key);
    }

    public boolean hasNBT(String key) {
        return nbt.containsKey(key);
    }

    private int getDefaultMaxDamage(ItemType type) {
        return switch (type.getId()) {
            case 268, 270, 271 -> 60;  // wooden tools
            case 272, 274, 275 -> 132; // stone tools
            case 267, 257, 258 -> 250; // iron tools
            case 276, 278, 279 -> 1561; // diamond tools
            case 261 -> 384; // bow
            case 298 -> 55;  // leather helmet
            case 299 -> 80;  // leather chestplate
            case 300 -> 75;  // leather leggings
            case 301 -> 65;  // leather boots
            case 306 -> 165; // iron helmet
            case 307 -> 240; // iron chestplate
            case 308 -> 225; // iron leggings
            case 309 -> 195; // iron boots
            case 310 -> 363; // diamond helmet
            case 311 -> 528; // diamond chestplate
            case 312 -> 495; // diamond leggings
            case 313 -> 429; // diamond boots
            default -> 0;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStack)) return false;
        ItemStack itemStack = (ItemStack) o;
        return type == itemStack.type && amount == itemStack.amount && damage == itemStack.damage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, amount, damage);
    }
}
