package net.vibmc.server;

import net.vibmc.command.CommandManager;
import net.vibmc.entity.EntityManager;
import net.vibmc.network.NetworkServer;
import net.vibmc.plugin.PluginManager;
import net.vibmc.plugin.event.ChatEvent;
import net.vibmc.plugin.event.PlayerJoinEvent;
import net.vibmc.plugin.event.PlayerQuitEvent;
import net.vibmc.plugin.event.TickEvent;
import net.vibmc.player.PlayerConnection;
import net.vibmc.player.PlayerManager;
import net.vibmc.scheduler.Scheduler;
import net.vibmc.storage.PlayerStore;
import net.vibmc.storage.WorldStore;
import net.vibmc.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final ServerConfig config;
    private final Scheduler scheduler;
    private final PluginManager pluginManager;
    private final CommandManager commandManager;
    private final PlayerManager playerManager;
    private final NetworkServer networkServer;
    private final World world;
    private final EntityManager entityManager;
    private final WorldStore worldStore;
    private final PlayerStore playerStore;
    private final AtomicLong tickCounter = new AtomicLong();
    private volatile boolean running;

    public Server(ServerConfig config) throws IOException {
        this.config = config;
        this.scheduler = new Scheduler();
        this.pluginManager = new PluginManager();
        this.commandManager = new CommandManager(this);
        this.playerManager = new PlayerManager(this);
        this.world = new World(config.seed(), config.worldName());
        this.entityManager = new EntityManager();
        this.networkServer = new NetworkServer(this, config.port());
        Path worldPath = Path.of(config.worldName());
        Files.createDirectories(worldPath);
        this.worldStore = new WorldStore(this.world, worldPath);
        this.playerStore = new PlayerStore(worldPath.resolve("players"));
    }

    public void start() throws Exception {
        running = true;
        File pluginsDir = new File("plugins");
        pluginManager.loadPlugins(pluginsDir, this);
        worldStore.loadAll();
        playerStore.loadAll(playerManager);
        networkServer.start();
        scheduler.scheduleRepeating(this::tick, 0L, 50L);
        LOGGER.info("vib-MC started on port " + config.port());
        while (running) {
            Thread.sleep(1000L);
        }
    }

    public void stop() {
        running = false;
        networkServer.shutdown();
        scheduler.shutdown();
        try {
            worldStore.saveAll();
            playerStore.saveAll(playerManager);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while saving world data", e);
        }
        pluginManager.shutdown();
        LOGGER.info("vib-MC stopped");
    }

    private void tick() {
        long tick = tickCounter.incrementAndGet();
        world.tick(tick);
        entityManager.tick();
        pluginManager.dispatch(new TickEvent(tick));
    }

    public void broadcast(String message) {
        playerManager.broadcast(message);
        pluginManager.dispatch(new ChatEvent(null, message));
    }

    public void joinPlayer(PlayerConnection connection) {
        playerManager.add(connection);
        pluginManager.dispatch(new PlayerJoinEvent(connection.player()));
    }

    public void disconnectPlayer(PlayerConnection connection, String reason) {
        playerManager.remove(connection.player().name());
        connection.disconnect(reason);
        pluginManager.dispatch(new PlayerQuitEvent(connection.player(), reason));
    }

    public ServerConfig config() {
        return config;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public CommandManager commandManager() {
        return commandManager;
    }

    public NetworkServer networkServer() {
        return networkServer;
    }

    public World world() {
        return world;
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    public PluginManager pluginManager() {
        return pluginManager;
    }

    public PlayerManager playerManager() {
        return playerManager;
    }
}
