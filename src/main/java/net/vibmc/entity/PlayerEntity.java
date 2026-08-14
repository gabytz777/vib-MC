package net.vibmc.entity;

import net.vibmc.inventory.Inventory;
import net.vibmc.item.ItemStack;
import net.vibmc.network.ClientConnection;
import net.vibmc.network.Packet;
import net.vibmc.network.PacketBuffer;
import net.vibmc.player.GameMode;
import net.vibmc.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerEntity extends Entity {
    private final ClientConnection connection;
    private final String username;
    private final Inventory inventory;
    private final Set<Long> sentChunks = new HashSet<>();
    private GameMode gameMode;
    private boolean flying;
    private boolean allowFlight;
    private int heldItemSlot;
    private int foodLevel;
    private float foodSaturation;
    private int loadedChunkX;
    private int loadedChunkZ;

    public PlayerEntity(World world, ClientConnection connection, String username, UUID uuid) {
        super(world, uuid);
        this.connection = connection;
        this.username = username;
        this.inventory = new Inventory("Inventory", 36);
        this.gameMode = GameMode.SURVIVAL;
        this.heldItemSlot = 0;
        this.foodLevel = 20;
        this.foodSaturation = 5.0f;
        this.x = 8.5;
        this.y = 0;
        this.z = 8.5;
    }

    public void spawnAtSpawn() {
        int[] spawn = getWorld().findDrySpawn(8, 8, 16);
        this.x = spawn[0] + 0.5;
        this.z = spawn[1] + 0.5;
        this.y = getWorld().getHighestSolidY(spawn[0], spawn[1]) + 1;
        this.onGround = true;
    }

    public void respawn() {
        setHealth(getMaxHealth());
        setFoodLevel(20);
        setFoodSaturation(5.0f);
        spawnAtSpawn();
        teleport(x, y, z);
    }

    @Override
    public void tick() {
        if (!alive) return;
        if (gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR && !onGround && !flying) {
            double below = getWorld().getHighestBlockY((int) Math.floor(x), (int) Math.floor(z)) + 1;
            if (y > below) {
                y -= 0.1;
            } else {
                y = below;
                onGround = true;
            }
        }
    }

    public void sendMessage(String message) {
        sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x0F; // Chat Message
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeString(message);
                b.writeByte(0); // position: chat
            }
        });
    }

    public void sendKeepAlive(long id) {
        sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x1F; // Keep Alive
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeLong(id);
            }
        });
    }

    public void teleport(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x2F; // Player Position And Look
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeDouble(x);
                b.writeDouble(y);
                b.writeDouble(z);
                b.writeFloat(yaw);
                b.writeFloat(pitch);
                b.writeByte(0);
                b.writeVarInt(0);
            }
        });
    }

    public void sendPacket(Packet packet) {
        if (connection != null) {
            connection.sendPacket(packet);
        }
    }

    public ClientConnection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getGameMode() {
        return gameMode.getId();
    }

    public GameMode getGameModeEnum() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x1E; // Change Game State
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeByte(3); // reason: change game mode
                b.writeFloat(gameMode.getId());
            }
        });
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public void setAllowFlight(boolean allowFlight) {
        this.allowFlight = allowFlight;
    }

    public int getHeldItemSlot() {
        return heldItemSlot;
    }

    public void setHeldItemSlot(int heldItemSlot) {
        this.heldItemSlot = heldItemSlot;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = Math.max(0, Math.min(20, foodLevel));
    }

    public float getFoodSaturation() {
        return foodSaturation;
    }

    public void setFoodSaturation(float foodSaturation) {
        this.foodSaturation = foodSaturation;
    }

    public void addItem(ItemStack item) {
        inventory.addItem(item);
    }

    public Set<Long> getSentChunks() {
        return sentChunks;
    }

    public int getLoadedChunkX() {
        return loadedChunkX;
    }

    public int getLoadedChunkZ() {
        return loadedChunkZ;
    }

    public void setLoadedChunk(int x, int z) {
        this.loadedChunkX = x;
        this.loadedChunkZ = z;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }
}
