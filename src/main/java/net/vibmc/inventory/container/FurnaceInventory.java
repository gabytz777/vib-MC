package net.vibmc.inventory.container;

import net.vibmc.inventory.Inventory;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;

public class FurnaceInventory extends Inventory {
    private int burnTime;
    private int cookTime;
    private int totalBurnTime;

    public FurnaceInventory() {
        super("Furnace", 3);
        this.burnTime = 0;
        this.cookTime = 0;
        this.totalBurnTime = 0;
    }

    public ItemStack getInputSlot() { return getSlot(0); }
    public ItemStack getFuelSlot() { return getSlot(1); }
    public ItemStack getOutputSlot() { return getSlot(2); }

    public void setInputSlot(ItemStack item) { setSlot(0, item); }
    public void setFuelSlot(ItemStack item) { setSlot(1, item); }
    public void setOutputSlot(ItemStack item) { setSlot(2, item); }

    public void tick() {
        if (burnTime > 0) {
            burnTime--;
            if (canSmelt()) {
                cookTime++;
                if (cookTime >= 200) {
                    smelt();
                    cookTime = 0;
                }
            }
        } else if (hasFuel() && canSmelt()) {
            consumeFuel();
        } else {
            cookTime = 0;
        }
    }

    private boolean canSmelt() {
        ItemStack input = getInputSlot();
        return input != null && !input.isEmpty() && hasSmeltingResult(input.getType());
    }

    private boolean hasFuel() {
        ItemStack fuel = getFuelSlot();
        return fuel != null && !fuel.isEmpty() && getFuelBurnTime(fuel.getType()) > 0;
    }

    private void consumeFuel() {
        ItemStack fuel = getFuelSlot();
        if (fuel == null || fuel.isEmpty()) return;
        totalBurnTime = getFuelBurnTime(fuel.getType());
        burnTime = totalBurnTime;
        fuel.setAmount(fuel.getAmount() - 1);
        if (fuel.getAmount() <= 0) {
            setFuelSlot(new ItemStack(ItemType.AIR, 0));
        }
    }

    private void smelt() {
        ItemStack input = getInputSlot();
        ItemType result = getSmeltingResult(input.getType());
        if (result == null) return;

        ItemStack output = getOutputSlot();
        if (output == null || output.isEmpty()) {
            setOutputSlot(new ItemStack(result, 1));
        } else if (output.getType() == result && output.getAmount() < result.getMaxStackSize()) {
            output.setAmount(output.getAmount() + 1);
        } else {
            return;
        }

        input.setAmount(input.getAmount() - 1);
        if (input.getAmount() <= 0) {
            setInputSlot(new ItemStack(ItemType.AIR, 0));
        }
    }

    private boolean hasSmeltingResult(ItemType input) {
        return getSmeltingResult(input) != null;
    }

    private ItemType getSmeltingResult(ItemType input) {
        return switch (input.getId()) {
            case 15 -> ItemType.IRON_INGOT;   // iron ore -> iron ingot
            case 56 -> ItemType.DIAMOND;        // diamond ore -> diamond
            case 363 -> ItemType.COOKED_BEEF;    // beef -> cooked beef
            case 365 -> ItemType.COOKED_CHICKEN; // chicken -> cooked chicken
            case 319 -> ItemType.COOKED_PORKCHOP; // porkchop -> cooked porkchop
            case 423 -> ItemType.COOKED_MUTTON;  // mutton -> cooked mutton
            default -> null;
        };
    }

    private int getFuelBurnTime(ItemType fuel) {
        return switch (fuel.getId()) {
            case 263 -> 1600;  // coal
            case 17 -> 300;    // log
            case 5 -> 300;     // planks
            case 280 -> 100;   // stick
            default -> 0;
        };
    }

    public int getBurnTime() { return burnTime; }
    public int getCookTime() { return cookTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
}
