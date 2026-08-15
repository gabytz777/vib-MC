package net.vibmc.server;

import net.vibmc.auth.ServerKeyPair;
import net.vibmc.auth.SessionAuthenticator;
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
    private final ServerKeyPair keyPair;
    private final SessionAuthenticator sessionAuthenticator;

    private volatile boolean running;
    /** Guards shutdown so it runs exactly once and to completion, whoever triggers it. */
    private final Object stopLock = new Object();
    private volatile boolean stopping;
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
        this.keyPair = new ServerKeyPair();
        this.sessionAuthenticator = new SessionAuthenticator();
    }

    /** RSA identity used for the online-mode login handshake. */
    public ServerKeyPair getKeyPair() {
        return keyPair;
    }

    public SessionAuthenticator getSessionAuthenticator() {
        return sessionAuthenticator;
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
        if (!checkSecurityConfig()) {
            running = false;
            return;
        }
        maybePromptSkinPlugin();
        try {
            networkServer.start(config.address(), config.port());
        } catch (IOException e) {
            logger.severe("Failed to start network server: %s", e);
            running = false;
            return;
        }

        // A world saved before dimensions existed has no way into the Nether or End, so
        // make sure the spawn area has a portal and the End has its exit. Existing terrain
        // is otherwise untouched.
        net.vibmc.world.PortalTravel.ensureSpawnPortal(worldManager.getMainWorld());
        net.vibmc.world.PortalTravel.ensureEndExitPortal(worldManager.getEnd());

        pluginManager.loadPlugins("plugins");
        pluginManager.onLoad();
        pluginManager.onEnable();

        commandManager.startConsole();

        Thread tickThread = new Thread(this::tickLoop, "Server Tick");
        tickThread.setDaemon(true);
        tickThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        logger.info("vib-MC started on %s:%d (seed %d)", config.address(), config.port(),
                worldManager.getMainWorld().seed());

        while (running) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Refuses to start on a configuration that would silently let anyone log in as anyone.
     *
     * <p>Legacy forwarding trusts whatever identity the proxy sends. That is fine when the
     * proxy authenticated the player, but with {@code online-mode=false} nothing anywhere
     * in the chain checks the account, so the combination is rejected rather than started
     * in a state the operator would reasonably believe is secure.
     *
     * @return true if it is safe to continue starting
     */
    private boolean checkSecurityConfig() {
        if (config.proxyLegacy() && !config.onlineMode()) {
            logger.severe("proxy-mode=legacy requires online-mode=true.");
            logger.severe("Legacy forwarding trusts the identity the proxy sends, so the proxy "
                    + "must be the thing doing the authenticating. With online-mode=false nobody "
                    + "checks the account at all and any client could claim any username.");
            logger.severe("Set online-mode=true, or set proxy-mode=none for a direct-connect server.");
            return false;
        }
        if (config.proxyLegacy() && config.proxyTrustedAddress().isEmpty()) {
            logger.warn("proxy-mode=legacy with a blank proxy-trusted-address: any host that can "
                    + "reach this port can claim any identity. Only do this if the port is "
                    + "firewalled to the proxy.");
        }
        if (!config.onlineMode()) {
            logger.warn("online-mode=false: players are not verified with Mojang and skins come "
                    + "from the skin-url settings rather than their real accounts.");
        }
        return true;
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
            for (net.vibmc.world.World world : worldManager.getWorlds()) {
                world.tick(tickCounter);
            }
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

    /**
     * Shuts the server down, finishing all shutdown work before the process is allowed to
     * exit.
     *
     * <p>{@code running} is cleared <em>last</em>, deliberately. This can be called from
     * the console thread or the JVM shutdown hook, while {@link #start} is parked waiting
     * for {@code running} to go false. Clearing it first would release the main thread,
     * let {@code main} return, and let the JVM tear down the daemon threads mid-save -
     * which silently lost both the shutdown kick and the world save.
     */
    public void stop() {
        synchronized (stopLock) {
            if (stopping) {
                return;
            }
            stopping = true;
            logger.info("Shutting down...");

            // Kick players before saving so they see why the server went away, rather than
            // timing out into a generic connection-lost screen.
            String message = config.shutdownMessage();
            for (net.vibmc.entity.PlayerEntity player : playerManager.getOnlinePlayers()) {
                player.kick(message);
            }

            if (config.saveOnStop()) {
                int written = worldManager.saveAll();
                logger.info("Saved %d chunk%s on shutdown", written, written == 1 ? "" : "s");
            }
            pluginManager.onDisable();
            networkServer.stop();
            logger.info("vib-MC stopped");

            running = false;
        }
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
