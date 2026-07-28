package net.vibmc.inventory;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;
import net.vibmc.server.VibMC;

public class PlayerInventory extends Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SIZE = 1;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + MAIN_SIZE + ARMOR_SIZE + OFFHAND_SIZE;
    public static final int CRAFTING_SIZE = 4;
    public static final int TOTAL_WITH_CRAFTING = TOTAL_SIZE + CRAFTING_SIZE + 1;

    private final PlayerEntity player;
    // Layout: 0-8 hotbar, 9-35 main, 36-39 armor, 40 offhand

    public PlayerInventory(PlayerEntity player) {
        super("Inventory", TOTAL_SIZE);
        this.player = player;
    }

    public ItemStack getHotbarSlot(int index) {
        return getSlot(index);
    }

    public ItemStack getMainSlot(int index) {
        return getSlot(HOTBAR_SIZE + index);
    }

    public ItemStack getArmorSlot(int index) {
        return getSlot(HOTBAR_SIZE + MAIN_SIZE + index);
    }

    public ItemStack getOffhandSlot() {
        return getSlot(HOTBAR_SIZE + MAIN_SIZE + ARMOR_SIZE);
    }

    public void setHotbarSlot(int index, ItemStack item) {
        setSlot(index, item);
    }

    public void setArmorSlot(int index, ItemStack item) {
        setSlot(HOTBAR_SIZE + MAIN_SIZE + index, item);
    }

    public ItemStack getHeldItem() {
        return getHotbarSlot(player.getHeldItemSlot());
    }

    public int getTotalArmorValue() {
        int value = 0;
        for (int i = 0; i < ARMOR_SIZE; i++) {
            ItemStack armor = getArmorSlot(i);
            if (armor != null && !armor.isEmpty()) {
                value += getArmorProtection(armor.getType());
            }
        }
        return value;
    }

    private int getArmorProtection(ItemType type) {
        return switch (type.getId()) {
            case 298 -> 1;  // leather helmet
            case 299 -> 3;  // leather chestplate
            case 300 -> 2;  // leather leggings
            case 301 -> 1;  // leather boots
            case 306 -> 2;  // iron helmet
            case 307 -> 6;  // iron chestplate
            case 308 -> 5;  // iron leggings
            case 309 -> 2;  // iron boots
            case 310 -> 3;  // diamond helmet
            case 311 -> 8;  // diamond chestplate
            case 312 -> 6;  // diamond leggings
            case 313 -> 3;  // diamond boots
            default -> 0;
        };
    }

    public void damageHeldItem() {
        ItemStack held = getHeldItem();
        if (held != null && held.isBreakable()) {
            held.setDamage(held.getDamage() + 1);
            if (held.getDamage() >= held.getMaxDamage()) {
                setHotbarSlot(player.getHeldItemSlot(), new ItemStack(ItemType.AIR, 0));
            }
        }
    }
}
