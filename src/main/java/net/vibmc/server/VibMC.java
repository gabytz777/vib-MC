package net.vibmc.server;

import net.vibmc.command.CommandManager;
import net.vibmc.network.NetworkServer;
import net.vibmc.player.PlayerManager;
import net.vibmc.plugin.PluginManager;
import net.vibmc.server.util.Logger;
import net.vibmc.world.WorldManager;

import java.io.IOException;
import java.io.Console;

public final class VibMC {
    private static VibMC instance;

    private final ServerConfig config;
    private final Logger logger;
    private final WorldManager worldManager;
    private final PluginManager pluginManager;
    private final PlayerManager playerManager;
    private final NetworkServer networkServer;
    private final CommandManager commandManager;

    private volatile boolean running;
    private long tickCounter;

    private VibMC() {
        instance = this;
        this.config = ServerConfig.load("server.properties");
        this.logger = new Logger("vib-MC");
        this.worldManager = new WorldManager(config);
        this.pluginManager = new PluginManager();
        this.playerManager = new PlayerManager();
        this.networkServer = new NetworkServer();
        this.commandManager = new CommandManager();
    }

    public static void main(String[] args) {
        VibMC server = new VibMC();
        server.start();
    }

    public static VibMC getInstance() {
        return instance;
    }

    public void start() {
        running = true;
        maybePromptSkinPlugin();
        try {
            networkServer.start(config.address(), config.port());
        } catch (IOException e) {
            logger.severe("Failed to start network server: %s", e);
            running = false;
            return;
        }

        pluginManager.loadPlugins("plugins");
        pluginManager.onLoad();
        pluginManager.onEnable();

        commandManager.startConsole();

        Thread tickThread = new Thread(this::tickLoop, "Server Tick");
        tickThread.setDaemon(true);
        tickThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        logger.info("vib-MC started on %s:%d (seed %d)", config.address(), config.port(), config.seed());

        while (running) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void maybePromptSkinPlugin() {
        if (config.hasSkinPluginSetting()) {
            return;
        }
        Console console = System.console();
        if (console == null) {
            config.enableSkinPlugin(true);
            logger.info("No interactive console detected; Skins plugin enabled by default.");
            return;
        }
        logger.info("Would you like to add this plugin? (y/n)");
        logger.info("  Skins plugin - lets players set a custom skin with /skin set <url>.");
        logger.info("  Skins apply to everyone online instantly and can be changed anytime.");
        String line = console.readLine();
        boolean enable = line != null && (line.trim().equalsIgnoreCase("y") || line.trim().equalsIgnoreCase("yes"));
        config.enableSkinPlugin(enable);
        if (enable) {
            logger.info("Skins plugin added. Use /skin set <url> in-game to change your skin.");
        } else {
            logger.info("Skins plugin skipped. You can add it later by setting skin-plugin-enabled=true in server.properties.");
        }
    }

    private void tickLoop() {
        while (running) {
            long start = System.currentTimeMillis();
            tickCounter++;
            pluginManager.fireTickStart();
            worldManager.getMainWorld().tick(tickCounter);
            playerManager.tickAll();
            networkServer.tick();
            pluginManager.fireTickEnd();

            if (tickCounter % 100 == 0) {
                long keepAlive = System.currentTimeMillis();
                for (net.vibmc.entity.PlayerEntity player : playerManager.getOnlinePlayers()) {
                    player.sendKeepAlive(keepAlive);
                }
            }

            int autosaveInterval = config.autosaveIntervalTicks();
            if (autosaveInterval > 0 && tickCounter % autosaveInterval == 0) {
                int written = worldManager.saveAll();
                if (written > 0) {
                    logger.info("Autosaved %d chunk%s", written, written == 1 ? "" : "s");
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            try {
                Thread.sleep(Math.max(1, 50 - elapsed));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        logger.info("Shutting down...");
        if (config.saveOnStop()) {
            int written = worldManager.saveAll();
            logger.info("Saved %d chunk%s on shutdown", written, written == 1 ? "" : "s");
        }
        pluginManager.onDisable();
        networkServer.stop();
        logger.info("vib-MC stopped");
    }

    public boolean isRunning() {
        return running;
    }

    public ServerConfig getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public NetworkServer getNetworkServer() {
        return networkServer;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
