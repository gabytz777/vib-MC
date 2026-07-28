package net.vibmc.entity;

import net.vibmc.server.util.Position;
import net.vibmc.world.World;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Entity {
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(1);

    protected final int entityId;
    protected final UUID uuid;
    protected World world;
    protected double x, y, z;
    protected float yaw, pitch;
    protected float health;
    protected float maxHealth;
    protected boolean alive;
    protected boolean onGround;
    protected boolean invulnerable;

    public Entity(World world) {
        this.entityId = ENTITY_ID_COUNTER.getAndIncrement();
        this.uuid = UUID.randomUUID();
        this.world = world;
        this.alive = true;
        this.health = 20.0f;
        this.maxHealth = 20.0f;
        this.onGround = false;
        this.invulnerable = false;
    }

    public abstract void tick();

    public int getEntityId() { return entityId; }
    public UUID getUuid() { return uuid; }

    public World getWorld() { return world; }
    public void setWorld(World world) { this.world = world; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public void setHealth(float health) { this.health = Math.max(0, Math.min(maxHealth, health)); }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }

    public void damage(float amount) {
        if (invulnerable || !alive) return;
        health -= amount;
        if (health <= 0) {
            health = 0;
            die();
        }
    }

    public void heal(float amount) {
        if (!alive) return;
        health = Math.min(maxHealth, health + amount);
    }

    public boolean isAlive() { return alive; }
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
    public boolean isInvulnerable() { return invulnerable; }
    public void setInvulnerable(boolean inv) { this.invulnerable = inv; }

    public void die() {
        if (!alive) return;
        alive = false;
        onDeath();
    }

    protected void onDeath() {
        // Override in subclasses
    }

    public void remove() {
        world.removeEntity(this);
    }

    public boolean isPlayer() { return false; }
}
