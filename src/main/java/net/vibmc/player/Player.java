package net.vibmc.player;

import net.vibmc.inventory.Inventory;
import net.vibmc.inventory.ItemStack;

public class Player {
    private final String name;
    private final Inventory inventory = new Inventory(36);
    private double x;
    private double y;
    private double z;
    private int health = 20;
    private int hunger = 20;
    private boolean sprinting;
    private boolean sneaking;
    private int experience;
    private GameMode gameMode = GameMode.SURVIVAL;

    public Player(String name) {
        this.name = name;
    }

    public void addItem(ItemStack itemStack) {
        inventory.addItem(itemStack);
    }

    public void damage(int amount) {
        health = Math.max(0, health - amount);
    }

    public void heal(int amount) {
        health = Math.min(20, health + amount);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String name() {
        return name;
    }

    public int health() {
        return health;
    }

    public int hunger() {
        return hunger;
    }

    public boolean sprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean sneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public GameMode gameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public Inventory inventory() {
        return inventory;
    }

    public int experience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }
}
