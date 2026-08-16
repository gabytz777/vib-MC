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

    /** Below this the player has left the world and is falling in the void. */
    private static final int VOID_DEATH_Y = -24;
    /** Blocks you can drop before it starts to hurt, as in vanilla. */
    private static final double FALL_DAMAGE_FREE_BLOCKS = 3.0;

    private int floatingTicks;
    private int dimensionChangeGraceTicks;
    private int portalTicks;
    private int portalCooldownTicks;
    /** Height at the previous tick, so the checks can tell falling from hovering. */
    private double lastY;
    /** Blocks dropped so far in the current fall. */
    private double fallDistance;
    /** Set on death, cleared when the client asks to respawn. */
    private boolean dead;

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

    /**
     * Brings the player back after death, in the overworld.
     *
     * <p>Dying in the Nether or the End sends you home, as vanilla does - and the client
     * needs a Respawn packet either way, because that is what takes the death screen down.
     */
    public void respawn() {
        setHealth(getMaxHealth());
        setFoodLevel(20);
        setFoodSaturation(5.0f);
        dead = false;

        World target = getWorld();
        net.vibmc.server.VibMC server = net.vibmc.server.VibMC.getInstance();
        if (server != null && server.getWorldManager().getMainWorld() != null) {
            target = server.getWorldManager().getMainWorld();
        }
        if (target != getWorld()) {
            getWorld().removeEntity(this);
            setWorld(target);
            target.addEntity(this);
        }

        int[] spawn = target.findDrySpawn(8, 8, 16);
        rebuildWorldForClient(target, spawn[0] + 0.5,
                target.getHighestSolidY(spawn[0], spawn[1]) + 1, spawn[1] + 0.5);
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

        double dropped = lastY - y;
        boolean descending = dropped > 0.005;
        lastY = y;

        if (dead) {
            return;  // waiting on the client to press respawn
        }
        if (y <= VOID_DEATH_Y) {
            die("fell out of the world");
            return;
        }
        tickPortal();
        tickFall(dropped, descending);
        tickFlightCheck(descending);
    }

    /**
     * Adds up how far the player has dropped, and bills them for it on landing.
     *
     * <p>Vanilla's rule: the first three blocks are free, and every block after that is
     * half a heart. Positions come from the client, so the distance is accumulated tick by
     * tick rather than trusting a single report.
     */
    private void tickFall(double dropped, boolean descending) {
        if (isFallExempt()) {
            fallDistance = 0;
            return;
        }
        // Landing is checked before descent, so walking down a hill settles up every tick
        // and never accumulates into one enormous bill at the bottom.
        if (onGround) {
            fallDistance += Math.max(0, dropped);
            land();
            return;
        }
        if (descending) {
            fallDistance += dropped;
            return;
        }
        fallDistance = 0;  // rising, or holding still in the air
    }

    private void land() {
        double distance = fallDistance;
        fallDistance = 0;
        int damage = (int) Math.floor(distance - FALL_DAMAGE_FREE_BLOCKS);
        if (damage <= 0 || landedSoftly()) {
            return;
        }
        damage(damage, "fell from a high place");
    }

    /** Water breaks a fall, as it should. */
    private boolean landedSoftly() {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int blockY = (int) Math.floor(y);
        for (int dy = 0; dy >= -1; dy--) {
            if (getWorld().getBlock(blockX, blockY + dy, blockZ) == net.vibmc.world.Block.WATER.id()) {
                return true;
            }
        }
        return false;
    }

    private boolean isFallExempt() {
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return true;
        }
        if (flying || dimensionChangeGraceTicks > 0) {
            return true;
        }
        return false;
    }

    /** Hurts the player, killing them if it takes the last of their health. */
    public void damage(float amount, String cause) {
        if (dead || !alive || invulnerable || amount <= 0) {
            return;
        }
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return;
        }
        setHealth(getHealth() - amount);

        net.vibmc.server.VibMC server = net.vibmc.server.VibMC.getInstance();
        if (getHealth() <= 0) {
            die(cause);
            return;
        }
        if (server != null) {
            server.getPlayerManager().sendHealth(this);
        }
    }

    @Override
    public void damage(float amount) {
        damage(amount, "died");
    }

    /**
     * Kills the player and puts the death screen up.
     *
     * <p>Sending zero health is what the client reacts to; it answers with a Client Status
     * packet when the player clicks respawn, which is where {@link #respawn()} picks up.
     */
    public void die(String cause) {
        if (dead) {
            return;
        }
        dead = true;
        setHealth(0);
        floatingTicks = 0;

        net.vibmc.server.VibMC server = net.vibmc.server.VibMC.getInstance();
        if (server != null) {
            server.getPlayerManager().sendHealth(this);
            server.getPlayerManager().broadcastMessage(
                    "{\"text\":\"§7" + username + " " + cause + "\"}");
        }
    }

    public boolean isDead() {
        return dead;
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
    private void tickFlightCheck(boolean descending) {
        if (isFlightExempt()) {
            floatingTicks = 0;
            return;
        }
        // Falling is not flying. Someone who broke the block under themselves, or stepped
        // off the edge of an End island, is on their way down - the check is for players
        // who stay up, so anyone losing height is left alone.
        if (descending || y < 0) {
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

        rebuildWorldForClient(destination, newX, newY, newZ);
    }

    /**
     * Puts the player somewhere and makes the client rebuild its world around them.
     *
     * <p>Shared by dimension travel and respawning, because to the client they are the
     * same event: a Respawn packet, then everything that packet just invalidated.
     */
    private void rebuildWorldForClient(World destination, double newX, double newY, double newZ) {
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.lastY = newY;
        this.onGround = true;
        // Arriving counts as a teleport: the client is mid-reload and its position reports
        // cannot be trusted for a moment.
        this.dimensionChangeGraceTicks = DIMENSION_CHANGE_GRACE_TICKS;
        this.portalCooldownTicks = net.vibmc.world.PortalTravel.COOLDOWN_TICKS;
        this.portalTicks = 0;
        this.floatingTicks = 0;
        this.fallDistance = 0;  // arriving somewhere is not a fall

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

        // The Respawn packet only tells the client to start over. Everything it needs to
        // finish - the new terrain, and above all the position that ends the "Downloading
        // terrain" screen - has to follow it immediately.
        net.vibmc.server.VibMC server = net.vibmc.server.VibMC.getInstance();
        if (server != null && connection != null) {
            server.getPlayerManager().sendDimensionChangePackets(this);
        }
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

    /**
     * The Player Abilities flags this player should have right now.
     *
     * <p>Derived from the game mode every time rather than stored, because the stored
     * version was the bug: a Respawn packet told the client "you are in creative", and the
     * abilities packet that followed was built from flags {@code /gamemode} never touched,
     * so the client showed a creative HUD with survival capabilities and no flight.
     *
     * <p>Bits are 0x01 invulnerable, 0x02 flying, 0x04 may fly, 0x08 creative.
     */
    public int abilityFlags() {
        switch (gameMode) {
            case SPECTATOR:
                return 0x01 | 0x02 | 0x04;
            case CREATIVE:
                return 0x01 | 0x04 | 0x08;
            default:
                int flags = 0;
                if (invulnerable) flags |= 0x01;
                if (flying) flags |= 0x02;
                if (allowFlight || serverAllowsFlight()) flags |= 0x04;
                return flags;
        }
    }

    private static boolean serverAllowsFlight() {
        net.vibmc.server.VibMC server = net.vibmc.server.VibMC.getInstance();
        return server != null && server.getConfig().allowFlight();
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
        // Sync abilities so the client actually flies/noclips.
        int finalFlags = abilityFlags();
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
