package net.vibmc.entity;

import net.vibmc.inventory.PlayerInventory;
import net.vibmc.item.ItemStack;
import net.vibmc.network.ClientConnection;
import net.vibmc.network.PacketBuffer;
import net.vibmc.server.VibMC;
import net.vibmc.server.util.Position;
import net.vibmc.world.World;
import net.vibmc.world.chunk.Chunk;
import net.vibmc.network.Packet;

public class PlayerEntity extends LivingEntity {
    private final ClientConnection connection;
    private final String username;
    private PlayerInventory inventory;
    private int heldItemSlot;
    private boolean sneaking;
    private boolean sprinting;
    private boolean flying;
    private boolean allowFlight;
    private boolean invulnerable;
    private int foodLevel;
    private float foodSaturation;
    private int experienceLevel;
    private float experienceProgress;
    private int gameMode;
    private long lastKeepAlive;
    private int ping;

    public PlayerEntity(ClientConnection connection) {
        super(VibMC.getInstance().getWorldManager().getMainWorld());
        this.connection = connection;
        this.username = connection.getUsername();
        this.inventory = new PlayerInventory(this);
        this.heldItemSlot = 0;
        this.sneaking = false;
        this.sprinting = false;
        this.flying = false;
        this.allowFlight = false;
        this.foodLevel = 20;
        this.foodSaturation = 5.0f;
        this.experienceLevel = 0;
        this.experienceProgress = 0;
        this.gameMode = 0; // survival by default
        this.maxHealth = 20.0f;
        this.health = 20.0f;
        this.lastKeepAlive = System.currentTimeMillis();

        // Spawn on surface
        int sx = world.getSpawnX();
        int sz = world.getSpawnZ();
        Chunk c = world.getChunk(sx >> 4, sz >> 4);
        int top = (c != null) ? c.getHighestBlockY(sx & 15, sz & 15) : world.getSpawnY();
        this.y = (top > Integer.MIN_VALUE) ? top + 1 : world.getSpawnY();
        this.x = sx + 0.5;
        this.z = sz + 0.5;
        this.onGround = true;
        this.motionY = 0;
    }

    @Override
    public void tick() {
        super.tick();

        // Keep alive
        long now = System.currentTimeMillis();
        if (now - lastKeepAlive > 5000) {
            sendKeepAlive();
            lastKeepAlive = now;
        }

        // Food/exhaustion
        if (sprinting) {
            addExhaustion(0.01f);
        }

        // Health regeneration
        if (foodLevel > 17 && health < maxHealth) {
            health = Math.min(maxHealth, health + 0.5f);
        }

        // Starvation
        if (foodLevel <= 0) {
            damage(1.0f);
        }
    }

    private void sendKeepAlive() {
        long id = System.currentTimeMillis();
        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x1F; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) { b.writeLong(id); }
        });
    }

    public void handleKeepAlive(long id) {
        // Keep alive response - calculate ping
    }

    public void sendMessage(String jsonMessage) {
        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x0F; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeString(jsonMessage);
                b.writeByte(0);
            }
        });
    }

    public void handleBlockBreak(long position, int face) {
        Position pos = Position.fromLong(position);
        var world = getWorld();
        world.breakBlock(pos);
    }

    public void performRespawn() {
        health = maxHealth;
        alive = true;
        onGround = true;
        motionX = 0;
        motionY = 0;
        motionZ = 0;
        foodLevel = 20;
        foodSaturation = 5.0f;
        x = world.getSpawnX();
        y = world.getSpawnY();
        z = world.getSpawnZ();

        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x35; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeInt(0);
                b.writeByte(1);
                b.writeByte(1);
                b.writeString("default");
            }
        });

        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x2F; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeDouble(x);
                b.writeDouble(y + 1.62);
                b.writeDouble(z);
                b.writeFloat(yaw);
                b.writeFloat(pitch);
                b.writeByte(0);
                b.writeVarInt(0);
            }
        });
    }

    public ClientConnection getConnection() { return connection; }
    public String getUsername() { return username; }
    public PlayerInventory getInventory() { return inventory; }
    public int getHeldItemSlot() { return heldItemSlot; }
    public void setHeldItemSlot(int slot) { this.heldItemSlot = slot; }
    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }
    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    public boolean isAllowFlight() { return allowFlight; }
    public void setAllowFlight(boolean allow) { this.allowFlight = allow; }

    public int getFoodLevel() { return foodLevel; }
    public void setFoodLevel(int level) { this.foodLevel = Math.max(0, Math.min(20, level)); }
    public float getFoodSaturation() { return foodSaturation; }
    public void setFoodSaturation(float sat) { this.foodSaturation = sat; }

    public void addExhaustion(float amount) {
        foodSaturation -= amount;
        if (foodSaturation < 0) {
            foodSaturation = 0;
            foodLevel--;
            updateFoodPackets();
        }
    }

    private void updateFoodPackets() {
        float hp = health;
        int level = foodLevel;
        float saturation = foodSaturation;
        connection.sendPacket(new Packet() {
            public int getPacketId() { return 0x41; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeFloat(hp);
                b.writeVarInt(level);
                b.writeFloat(saturation);
            }
        });
    }

    public int getExperienceLevel() { return experienceLevel; }
    public float getExperienceProgress() { return experienceProgress; }
    public void setExperienceLevel(int level) { this.experienceLevel = level; }
    public void setExperienceProgress(float progress) { this.experienceProgress = progress; }

    public int getGameMode() { return gameMode; }
    public void setGameMode(int gameMode) { this.gameMode = gameMode; }

    public int getPing() { return ping; }
    public void setPing(int ping) { this.ping = ping; }

    @Override
    public boolean isPlayer() { return true; }

    @Override
    protected void handleFallDamage(double fallDistance) {
        if (gameMode == 1) return; // creative mode: no fall damage
        super.handleFallDamage(fallDistance);
    }

    @Override
    protected void onDeath() {
        sendMessage("{\"text\":\"§cYou died!\"}");
        // Don't auto-respawn - client triggers respawn via Client Status (0x03, action 0)
    }
}
