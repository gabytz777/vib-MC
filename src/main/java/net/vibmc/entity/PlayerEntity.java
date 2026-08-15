package net.vibmc.entity;

import net.vibmc.auth.GameProfile;
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
    private GameProfile profile;
    private GameMode gameMode;
    private boolean flying;
    private boolean allowFlight;
    private int heldItemSlot;
    private int foodLevel;
    private float foodSaturation;
    private int loadedChunkX;
    private int loadedChunkZ;

    /**
     * How long the player has been unsupported in the air. Vanilla tolerates a good while
     * before acting, because normal play (jumping, falling, lag) leaves players airborne
     * constantly; only a sustained hover is treated as flying.
     */
    private static final int FLOATING_KICK_TICKS = 80;
    /** Ticks after a teleport or dimension change during which position is not judged. */
    public static final int DIMENSION_CHANGE_GRACE_TICKS = 100;

    private int floatingTicks;
    private int dimensionChangeGraceTicks;
    private int portalTicks;
    private int portalCooldownTicks;

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
        if (dimensionChangeGraceTicks > 0) {
            dimensionChangeGraceTicks--;
        }
        if (portalCooldownTicks > 0) {
            portalCooldownTicks--;
        }
        tickPortal();
        tickFlightCheck();
    }

    /**
     * Fires a portal once the player has stood in it long enough, then puts the trip on
     * cooldown so they do not immediately bounce back through the one they arrive next to.
     */
    private void tickPortal() {
        if (portalCooldownTicks > 0) {
            portalTicks = 0;
            return;
        }
        if (!net.vibmc.world.PortalTravel.inPortal(this)) {
            portalTicks = 0;
            return;
        }
        portalTicks++;
        if (portalTicks < net.vibmc.world.PortalTravel.ACTIVATION_TICKS) {
            return;
        }
        portalTicks = 0;
        net.vibmc.world.Dimension destination = net.vibmc.world.PortalTravel.destinationFor(this);
        if (destination != null) {
            net.vibmc.world.PortalTravel.travel(this, destination);
        }
    }

    /**
     * The floating check, in the spirit of vanilla's: a player who is neither on the ground
     * nor entitled to fly, and who stays that way, is kicked.
     *
     * <p>Deliberately lenient and deliberately narrow. It only looks at whether the player
     * is unsupported over a long window - no speed, reach, or heuristic checks - so normal
     * play and ordinary lag never trip it.
     */
    private void tickFlightCheck() {
        if (isFlightExempt()) {
            floatingTicks = 0;
            return;
        }
        if (onGround || isSupported()) {
            floatingTicks = 0;
            return;
        }
        floatingTicks++;
        if (floatingTicks > FLOATING_KICK_TICKS) {
            floatingTicks = 0;
            kick("Flying is not enabled on this server");
        }
    }

    private boolean isFlightExempt() {
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return true;
        }
        if (allowFlight || flying) {
            return true;
        }
        if (dimensionChangeGraceTicks > 0) {
            return true;
        }
        // Server config can allow flight outright.
        net.vibmc.server.VibMC server = net.vibmc.server.VibMC.getInstance();
        return server != null && server.getConfig().allowFlight();
    }

    /**
     * Whether there is anything under the player that could be holding them up. Checked a
     * couple of blocks down and in liquid, so swimming, ladders-adjacent play and small
     * desyncs do not read as flight.
     */
    private boolean isSupported() {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int blockY = (int) Math.floor(y);

        for (int dy = 0; dy >= -3; dy--) {
            short block = getWorld().getBlock(blockX, blockY + dy, blockZ);
            if (block != net.vibmc.world.Block.AIR.id()) {
                return true;
            }
        }
        return false;
    }

    /** Disconnects this player with a message. */
    public void kick(String reason) {
        if (connection != null) {
            connection.disconnect(reason);
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
        // The client is about to be moved out from under itself; its next few position
        // reports describe where it used to be, so do not judge them as flight.
        this.dimensionChangeGraceTicks = DIMENSION_CHANGE_GRACE_TICKS;
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

    /**
     * Moves this player into another world.
     *
     * <p>The client is told with a Respawn packet, which makes it tear down its current
     * world and start a fresh one - so every chunk it had is now stale and the sent-chunk
     * set has to be cleared, or the streamer would never resend them. The player is then
     * positioned and their surroundings streamed in for the new dimension.
     */
    public void changeDimension(World destination, double newX, double newY, double newZ) {
        World previous = getWorld();
        if (previous == destination) {
            teleport(newX, newY, newZ);
            return;
        }

        previous.removeEntity(this);
        setWorld(destination);
        destination.addEntity(this);

        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.onGround = true;
        // Arriving counts as a teleport: the client is mid-reload and its position reports
        // cannot be trusted for a moment.
        this.dimensionChangeGraceTicks = DIMENSION_CHANGE_GRACE_TICKS;
        this.portalCooldownTicks = net.vibmc.world.PortalTravel.COOLDOWN_TICKS;
        this.portalTicks = 0;

        int dimensionId = destination.dimension().protocolId();
        int gameModeId = gameMode.getId();
        sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x35; // Respawn
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeInt(dimensionId);
                b.writeByte(1); // difficulty
                b.writeByte(gameModeId);
                b.writeString("default"); // level type
            }
        });

        sentChunks.clear();
        loadedChunkX = Integer.MIN_VALUE;
        loadedChunkZ = Integer.MIN_VALUE;
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

    /** The authenticated (or offline) identity this player logged in with. */
    public GameProfile getProfile() {
        return profile;
    }

    public void setProfile(GameProfile profile) {
        this.profile = profile;
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
        // sync abilities so the client actually flies/noclips: spectator = invulnerable + flying,
        // creative = can fly
        int flags = 0;
        if (gameMode == GameMode.SPECTATOR) {
            flags |= 0x01 | 0x02 | 0x04;
        } else if (gameMode == GameMode.CREATIVE) {
            flags |= 0x04;
        }
        int finalFlags = flags;
        sendPacket(new Packet() {
            @Override
            public int getPacketId() {
                return 0x2C; // Player Abilities
            }

            @Override
            public void read(PacketBuffer b) {
            }

            @Override
            public void write(PacketBuffer b) {
                b.writeByte(finalFlags);
                b.writeFloat(0.05f);
                b.writeFloat(0.1f);
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
