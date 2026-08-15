package net.vibmc.player;

import net.vibmc.auth.GameProfile;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.network.ClientConnection;
import net.vibmc.server.ServerConfig;
import net.vibmc.network.PacketBuffer;
import net.vibmc.plugin.event.ChatEvent;
import net.vibmc.plugin.event.PlayerJoinEvent;
import net.vibmc.plugin.event.PlayerQuitEvent;
import net.vibmc.plugin.PluginManager;
import net.vibmc.server.VibMC;
import net.vibmc.world.Chunk;
import net.vibmc.world.World;
import net.vibmc.network.Packet;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private final Map<UUID, PlayerEntity> players;
    private final Map<String, PlayerEntity> byName;

    public PlayerManager() {
        this.players = new ConcurrentHashMap<>();
        this.byName = new ConcurrentHashMap<>();
    }

    /**
     * Adds a player to the online registry, without any networking.
     *
     * <p>Kept separate from {@link #addPlayer} so the lookup tables have one owner and can
     * be reasoned about (and tested) without a live connection behind every player.
     */
    public void register(PlayerEntity player) {
        players.put(player.getUuid(), player);
        byName.put(player.getUsername().toLowerCase(), player);
    }

    /** Removes a player from the online registry, without any networking. */
    public void unregister(PlayerEntity player) {
        players.remove(player.getUuid());
        byName.remove(player.getUsername().toLowerCase());
    }

    public void addPlayer(PlayerEntity player) {
        register(player);

        VibMC server = VibMC.getInstance();
        PluginManager pluginManager = server.getPluginManager();

        PlayerJoinEvent event = new PlayerJoinEvent(player, player.getUsername() + " joined the game");
        pluginManager.fireEvent(event);

        server.getLogger().info("%s joined the game", player.getUsername());

        server.getLogger().info("Player spawn at %.1f, %.1f, %.1f (chunk %d, %d)", player.getX(), player.getY(), player.getZ(), (int) Math.floor(player.getX()) >> 4, (int) Math.floor(player.getZ()) >> 4);
        sendJoinPackets(player);
        sendPlayerInfo(player.getConnection(), 0, players.values());
        for (PlayerEntity other : players.values()) {
            if (other != player) {
                sendPlayerInfo(other.getConnection(), 0, Collections.singletonList(player));
            }
        }
        broadcastMessage("{\"text\":\"§e" + player.getUsername() + " joined the game\"}");
    }

    public void removePlayer(PlayerEntity player) {
        unregister(player);

        VibMC server = VibMC.getInstance();
        PluginManager pluginManager = server.getPluginManager();

        if (player.getUsername() != null && !player.getUsername().isEmpty()) {
            PlayerQuitEvent event = new PlayerQuitEvent(player, player.getUsername() + " left the game");
            pluginManager.fireEvent(event);

            server.getLogger().info("%s left the game", player.getUsername());
            broadcastMessage("{\"text\":\"§e" + player.getUsername() + " left the game\"}");
            for (PlayerEntity other : players.values()) {
                sendPlayerInfo(other.getConnection(), 4, Collections.singletonList(player));
            }
        }
    }

    public PlayerEntity getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public PlayerEntity getPlayer(String name) {
        return byName.get(name.toLowerCase());
    }

    public PlayerEntity getPlayer(ClientConnection connection) {
        for (PlayerEntity player : players.values()) {
            if (player.getConnection() == connection) {
                return player;
            }
        }
        return null;
    }

    public Collection<PlayerEntity> getOnlinePlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public int getOnlineCount() {
        return players.size();
    }

    public void refreshSkin(PlayerEntity target) {
        for (PlayerEntity other : players.values()) {
            sendPlayerInfo(other.getConnection(), 4, Collections.singletonList(target));
            sendPlayerInfo(other.getConnection(), 0, Collections.singletonList(target));
        }
    }

    public void broadcastMessage(String message) {
        for (PlayerEntity player : players.values()) {
            player.sendMessage(message);
        }
    }

    public void broadcastMessage(String message, PlayerEntity exclude) {
        for (PlayerEntity player : players.values()) {
            if (player != exclude) {
                player.sendMessage(message);
            }
        }
    }

    public void handleChat(PlayerEntity sender, String message) {
        if (message.startsWith("/")) {
            VibMC.getInstance().getCommandManager().execute(new CommandSender(sender), message);
            return;
        }

        ChatEvent event = new ChatEvent(sender, message);
        VibMC.getInstance().getPluginManager().fireEvent(event);
        if (event.isCancelled()) return;

        String formatted = "{\"text\":\"<" + sender.getUsername() + "> " + event.getMessage() + "\"}";
        broadcastMessage(formatted);
    }

    public void tickAll() {
        for (PlayerEntity player : players.values()) {
            player.tick();
            updateChunkStream(player);
        }
    }

    private void updateChunkStream(PlayerEntity player) {
        int cx = (int) Math.floor(player.getX()) >> 4;
        int cz = (int) Math.floor(player.getZ()) >> 4;
        if (cx == player.getLoadedChunkX() && cz == player.getLoadedChunkZ()) {
            return;
        }
        int viewDist = VibMC.getInstance().getConfig().getViewDistance();
        Set<Long> wanted = new HashSet<>();
        for (int dx = -viewDist; dx <= viewDist; dx++) {
            for (int dz = -viewDist; dz <= viewDist; dz++) {
                wanted.add(chunkKey(cx + dx, cz + dz));
            }
        }
        for (Long key : new ArrayList<>(player.getSentChunks())) {
            if (!wanted.contains(key)) {
                // Not sending an actual Unload Chunk packet here: neither 0x1D nor 0x1C
                // is the real protocol-340 id (both caused real-client decode crashes -
                // see crash investigation) and the correct id hasn't been confirmed yet.
                // The client just keeps rendering these chunks a bit longer, which costs
                // a little extra client memory but is otherwise harmless.
                player.getSentChunks().remove(key);
            }
        }
        for (Long key : wanted) {
            if (player.getSentChunks().add(key)) {
                sendChunk(player.getConnection(), player.getWorld(),
                        (int) (key >> 32), (int) (key & 0xFFFFFFFFL));
            }
        }
        player.setLoadedChunk(cx, cz);
    }

    private static long chunkKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private void sendJoinPackets(PlayerEntity player) {
        ClientConnection conn = player.getConnection();
        World world = player.getWorld();

        sendLoginPlay(conn, player);
        sendDifficulty(conn);
        sendPlayerAbilities(conn, player);
        sendHeldItemChange(conn, player);
        sendWorldInfo(conn, player);
        sendSpawnPosition(conn, player);
        sendUpdateHealth(conn, player);
        sendPlayerPosition(conn, player);

        // Send spawn chunks AFTER positioning so the client requests around the right center
        int centerX = (int) Math.floor(player.getX()) >> 4;
        int centerZ = (int) Math.floor(player.getZ()) >> 4;
        int viewDist = VibMC.getInstance().getConfig().getViewDistance();
        for (int dx = -viewDist; dx <= viewDist; dx++) {
            for (int dz = -viewDist; dz <= viewDist; dz++) {
                sendChunk(conn, world, centerX + dx, centerZ + dz);
                player.getSentChunks().add(chunkKey(centerX + dx, centerZ + dz));
            }
        }
        player.setLoadedChunk(centerX, centerZ);

        sendGameState(conn);
    }

    /**
     * The textures blob to advertise for a player, and its signature when there is one.
     *
     * <p>An explicit {@code /skin} override wins, because that is a deliberate choice by
     * an operator. Otherwise an authenticated player keeps the real, Mojang-signed skin
     * from their profile - re-signing is impossible, so the signature is passed through
     * untouched and vanilla clients render the skin normally. A configured global
     * {@code skin-url} is the last resort, and is necessarily unsigned.
     *
     * @return {@code {value, signature}}, signature possibly null, or null for no textures
     */
    private String[] texturesProperty(PlayerEntity player) {
        ServerConfig config = VibMC.getInstance().getConfig();
        if (config.skinPluginEnabled()) {
            String override = config.skinUrlOverrideFor(player.getUsername());
            if (!override.isEmpty()) {
                return new String[]{encodeSkinUrl(player, override), null};
            }
        }
        GameProfile profile = player.getProfile();
        if (profile != null && profile.hasTextures()) {
            return new String[]{profile.texturesValue(), profile.texturesSignature()};
        }
        if (config.skinPluginEnabled()) {
            String url = config.skinUrl();
            if (!url.isEmpty()) {
                return new String[]{encodeSkinUrl(player, url), null};
            }
        }
        return null;
    }

    private static String encodeSkinUrl(PlayerEntity player, String url) {
        String json = "{\"timestamp\":" + System.currentTimeMillis()
                + ",\"profileId\":\"" + player.getUuid() + "\""
                + ",\"profileName\":\"" + player.getUsername() + "\""
                + ",\"textures\":{\"SKIN\":{\"url\":\"" + url.replace("\"", "") + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private void sendPlayerInfo(ClientConnection conn, int action, Collection<PlayerEntity> targets) {
        List<PlayerEntity> list = new ArrayList<>(targets);
        conn.sendPacket(new Packet() {
            // Player List Item is 0x2E at protocol 340 (1.12.2), not 0x04 (that's
            // Spawn Painting - the wrong ID here caused the client to try parsing
            // this packet's skin-texture payload as a painting motive name, which
            // has a short max-length string field, hence "received string length
            // is longer than maximum allowed (45 > 13)").
            public int getPacketId() { return 0x2E; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeVarInt(action);
                b.writeVarInt(list.size());
                for (PlayerEntity p : list) {
                    UUID uuid = p.getUuid();
                    b.writeLong(uuid.getMostSignificantBits());
                    b.writeLong(uuid.getLeastSignificantBits());
                    if (action == 0) {
                        b.writeString(p.getUsername());
                        String[] textures = texturesProperty(p);
                        if (textures == null) {
                            b.writeVarInt(0);
                        } else {
                            b.writeVarInt(1);
                            b.writeString("textures");
                            b.writeString(textures[0]);
                            // Signed properties keep Mojang's signature so the client
                            // trusts the skin; locally configured ones have none.
                            if (textures[1] == null) {
                                b.writeBoolean(false);
                            } else {
                                b.writeBoolean(true);
                                b.writeString(textures[1]);
                            }
                        }
                        b.writeVarInt(p.getGameMode());
                        b.writeVarInt(0);
                        b.writeBoolean(false);
                    }
                }
            }
        });
    }

    private void sendLoginPlay(ClientConnection conn, PlayerEntity player) {
        int entityId = player.getEntityId();
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x23; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeInt(entityId);
                b.writeByte(player.getGameMode());
                b.writeInt(player.getWorld().dimension().protocolId());
                b.writeByte(1);
                b.writeByte((byte) VibMC.getInstance().getConfig().getMaxPlayers());
                b.writeString("default");
                b.writeBoolean(false);
            }
        });
    }

    private void sendDifficulty(ClientConnection conn) {
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x0D; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeByte(1);
            }
        });
    }

    private void sendPlayerAbilities(ClientConnection conn, PlayerEntity player) {
        byte flags = 0;
        if (player.isInvulnerable()) flags |= 0x01;
        if (player.isFlying()) flags |= 0x02;
        if (player.isAllowFlight()) flags |= 0x04;
        byte finalFlags = flags;
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x2C; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeByte(finalFlags);
                b.writeFloat(0.05f);
                b.writeFloat(0.1f);
            }
        });
    }

    private void sendHeldItemChange(ClientConnection conn, PlayerEntity player) {
        byte slot = (byte) player.getHeldItemSlot();
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x3A; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) { b.writeByte(slot); }
        });
    }

    private void sendPlayerPosition(ClientConnection conn, PlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x2F; }
            public void read(PacketBuffer b) {}
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

    private void sendWorldInfo(ClientConnection conn, PlayerEntity player) {
        long worldTime = player.getWorld().getWorldTime();
        long dayTime = player.getWorld().getDayTime();
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x47; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeLong(worldTime);
                b.writeLong(dayTime);
            }
        });
    }

    private void sendSpawnPosition(ClientConnection conn, PlayerEntity player) {
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x46; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writePosition((int) player.getX(), (int) player.getY(), (int) player.getZ());
            }
        });
    }

    private void sendUpdateHealth(ClientConnection conn, PlayerEntity player) {
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x41; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeFloat(player.getHealth());
                b.writeVarInt(player.getFoodLevel());
                b.writeFloat(player.getFoodSaturation());
            }
        });
    }

    private void sendChunk(ClientConnection conn, World world, int chunkX, int chunkZ) {
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) return;

        // 1.12.2 sends the chunk data raw (no zlib layer); only network-level compression applies
        byte[] chunkData = chunk.toNetworkData();
        VibMC.getInstance().getLogger().info("Sending chunk %d,%d: raw=%d bytes", chunkX, chunkZ, chunkData.length);
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x20; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeInt(chunkX);
                b.writeInt(chunkZ);
                b.writeBoolean(true);
                b.writeVarInt(65535);
                b.writeVarInt(chunkData.length);
                b.writeBytes(chunkData);
                b.writeVarInt(0); // block entities (none)
            }
        });
    }

    private void sendGameState(ClientConnection conn) {
        conn.sendPacket(new Packet() {
            public int getPacketId() { return 0x1E; }
            public void read(PacketBuffer b) {}
            public void write(PacketBuffer b) {
                b.writeByte(1);
                b.writeFloat(0.0f);
            }
        });
    }

}
