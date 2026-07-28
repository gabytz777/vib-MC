package net.vibmc.server;

import net.vibmc.command.CommandManager;
import net.vibmc.config.ServerConfig;
import net.vibmc.network.NetworkServer;
import net.vibmc.plugin.PluginManager;
import net.vibmc.scheduler.VibMCScheduler;
import net.vibmc.world.WorldManager;
import net.vibmc.server.util.Logger;
import net.vibmc.server.util.CrashReporter;
import net.vibmc.player.PlayerManager;
import net.vibmc.metrics.MetricsManager;

import java.io.IOException;

public class VibMC {
    private static VibMC instance;
    private final ServerConfig config;
    private final Logger logger;
    private final VibMCScheduler scheduler;
    private final NetworkServer networkServer;
    private final WorldManager worldManager;
    private final CommandManager commandManager;
    private final PluginManager pluginManager;
    private final PlayerManager playerManager;
    private final MetricsManager metricsManager;
    private volatile boolean running;

    public VibMC(String[] args) {
        instance = this;
        this.logger = new Logger();
        this.config = new ServerConfig();
        this.scheduler = new VibMCScheduler();
        this.worldManager = new WorldManager();
        this.commandManager = new CommandManager();
        this.pluginManager = new PluginManager();
        this.playerManager = new PlayerManager();
        this.networkServer = new NetworkServer();
        this.metricsManager = new MetricsManager();
    }

    public void start() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting vib-MC server version 1.0.0");
        logger.info("Loading configuration...");
        config.load();

        logger.info("Loading plugins...");
        pluginManager.loadPlugins(config.getPluginDirectory());
        pluginManager.onLoad();

        logger.info("Starting scheduler...");
        scheduler.start();

        logger.info("Preparing world \"%s\"...", config.getWorldName());
        worldManager.initialize(config.getWorldName(), config.getSeed());

        logger.info("Starting network server on %s:%d...", config.getBindAddress(), config.getPort());
        try {
            networkServer.start(config.getBindAddress(), config.getPort());
        } catch (IOException e) {
            logger.severe("Failed to start network server: %s", e.getMessage());
            shutdown();
            return;
        }

        logger.info("Enabling plugins...");
        pluginManager.onEnable();

        running = true;
        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Done (%dms)! For help, type \"help\"", elapsed);

        logger.info("Starting console handler...");
        commandManager.startConsole();

        metricsManager.start();

        mainLoop();
    }

    private void mainLoop() {
        long lastTick = System.nanoTime();
        long tickTimeNs = 50_000_000L;
        int tickCount = 0;
        long secondTimer = System.currentTimeMillis();
        int tps = 0;

        while (running) {
            long now = System.nanoTime();
            long wait = tickTimeNs - (now - lastTick);
            if (wait > 0) {
                try {
                    long waitMs = wait / 1_000_000L;
                    int waitNs = (int) (wait % 1_000_000L);
                    Thread.sleep(waitMs, waitNs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            long tickStart = System.nanoTime();
            tick();
            long tickEnd = System.nanoTime();
            long tickDuration = (tickEnd - tickStart) / 1_000_000L;

            if (tickDuration > 100) {
                logger.warn("Tick took %dms (threshold: 100ms)", tickDuration);
            }

            lastTick += tickTimeNs;
            tickCount++;

            long nowMs = System.currentTimeMillis();
            if (nowMs - secondTimer >= 1000) {
                tps = tickCount;
                tickCount = 0;
                secondTimer = nowMs;
                metricsManager.recordTps(tps);
            }
        }
    }

    private void tick() {
        try {
            pluginManager.fireTickStart();
            scheduler.tick();
            worldManager.tick();
            playerManager.tickAll();
            networkServer.tick();
            pluginManager.fireTickEnd();
        } catch (Exception e) {
            logger.severe("Error during tick: %s", e.getMessage());
            CrashReporter.generateCrashReport(e, "Server tick");
        }
    }

    public void shutdown() {
        logger.info("Shutting down server...");
        running = false;

        logger.info("Saving worlds...");
        worldManager.saveAll();

        logger.info("Disabling plugins...");
        pluginManager.onDisable();

        logger.info("Stopping network server...");
        networkServer.stop();

        logger.info("Stopping scheduler...");
        scheduler.stop();

        logger.info("Server shutdown complete.");
        System.exit(0);
    }

    public static VibMC getInstance() {
        return instance;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    public VibMCScheduler getScheduler() {
        return scheduler;
    }

    public NetworkServer getNetworkServer() {
        return networkServer;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public MetricsManager getMetricsManager() {
        return metricsManager;
    }

    public boolean isRunning() {
        return running;
    }

    public static void main(String[] args) {
        VibMC server = new VibMC(args);
        server.start();
    }
}
