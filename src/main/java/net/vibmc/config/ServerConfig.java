package net.vibmc.config;

import net.vibmc.server.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig {
    private final Properties properties;
    private File configFile;
    private String motd = "A vib-MC Server";
    private int maxPlayers = 20;
    private long seed = 0;
    private String difficulty = "easy";
    private int viewDistance = 8;
    private int simulationDistance = 8;
    private boolean onlineMode = false;
    private int port = 25565;
    private String bindAddress = "0.0.0.0";
    private String worldName = "world";
    private String pluginDirectory = "plugins";
    private int maxTickTime = 60000;
    private boolean allowNether = true;
    private boolean allowEnd = true;
    private String levelType = "default";
    private boolean generateStructures = true;
    private boolean allowFlight = false;
    private boolean pvp = true;
    private boolean spawnAnimals = true;
    private boolean spawnMonsters = true;
    private int maxBuildHeight = 320;
    private String resourcePack = "";
    private String resourcePackSha1 = "";

    public ServerConfig() {
        this.properties = new Properties();
    }

    public void load() {
        configFile = new File("server.properties");
        if (!configFile.exists()) {
            createDefault();
        }
        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
        } catch (IOException e) {
            Logger logger = new Logger();
            logger.warn("Could not load server.properties, using defaults: %s", e.getMessage());
        }
        apply();
    }

    private void createDefault() {
        properties.setProperty("motd", motd);
        properties.setProperty("max-players", String.valueOf(maxPlayers));
        properties.setProperty("seed", String.valueOf(seed));
        properties.setProperty("difficulty", difficulty);
        properties.setProperty("view-distance", String.valueOf(viewDistance));
        properties.setProperty("simulation-distance", String.valueOf(simulationDistance));
        properties.setProperty("online-mode", String.valueOf(onlineMode));
        properties.setProperty("server-port", String.valueOf(port));
        properties.setProperty("server-ip", bindAddress);
        properties.setProperty("level-name", worldName);
        properties.setProperty("max-tick-time", String.valueOf(maxTickTime));
        properties.setProperty("allow-nether", String.valueOf(allowNether));
        properties.setProperty("allow-end", String.valueOf(allowEnd));
        properties.setProperty("level-type", levelType);
        properties.setProperty("generate-structures", String.valueOf(generateStructures));
        properties.setProperty("allow-flight", String.valueOf(allowFlight));
        properties.setProperty("pvp", String.valueOf(pvp));
        properties.setProperty("spawn-animals", String.valueOf(spawnAnimals));
        properties.setProperty("spawn-monsters", String.valueOf(spawnMonsters));
        properties.setProperty("max-build-height", String.valueOf(maxBuildHeight));
        properties.setProperty("resource-pack", resourcePack);
        properties.setProperty("resource-pack-sha1", resourcePackSha1);
        save();
    }

    public void save() {
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "vib-MC server properties");
        } catch (IOException e) {
            new Logger().warn("Could not save server.properties: %s", e.getMessage());
        }
    }

    private void apply() {
        motd = properties.getProperty("motd", motd);
        maxPlayers = parseInt(properties.getProperty("max-players"), maxPlayers);
        seed = parseLong(properties.getProperty("seed"), seed);
        difficulty = properties.getProperty("difficulty", difficulty);
        viewDistance = parseInt(properties.getProperty("view-distance"), viewDistance);
        simulationDistance = parseInt(properties.getProperty("simulation-distance"), simulationDistance);
        onlineMode = parseBool(properties.getProperty("online-mode"), onlineMode);
        port = parseInt(properties.getProperty("server-port"), port);
        bindAddress = properties.getProperty("server-ip", bindAddress);
        worldName = properties.getProperty("level-name", worldName);
        maxTickTime = parseInt(properties.getProperty("max-tick-time"), maxTickTime);
        allowNether = parseBool(properties.getProperty("allow-nether"), allowNether);
        allowEnd = parseBool(properties.getProperty("allow-end"), allowEnd);
        levelType = properties.getProperty("level-type", levelType);
        generateStructures = parseBool(properties.getProperty("generate-structures"), generateStructures);
        allowFlight = parseBool(properties.getProperty("allow-flight"), allowFlight);
        pvp = parseBool(properties.getProperty("pvp"), pvp);
        spawnAnimals = parseBool(properties.getProperty("spawn-animals"), spawnAnimals);
        spawnMonsters = parseBool(properties.getProperty("spawn-monsters"), spawnMonsters);
        maxBuildHeight = parseInt(properties.getProperty("max-build-height"), maxBuildHeight);
        resourcePack = properties.getProperty("resource-pack", resourcePack);
        resourcePackSha1 = properties.getProperty("resource-pack-sha1", resourcePackSha1);
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long parseLong(String value, long def) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private boolean parseBool(String value, boolean def) {
        if (value == null) return def;
        return Boolean.parseBoolean(value);
    }

    public String getMotd() { return motd; }
    public int getMaxPlayers() { return maxPlayers; }
    public long getSeed() { return seed; }
    public String getDifficulty() { return difficulty; }
    public int getViewDistance() { return viewDistance; }
    public int getSimulationDistance() { return simulationDistance; }
    public boolean isOnlineMode() { return onlineMode; }
    public int getPort() { return port; }
    public String getBindAddress() { return bindAddress; }
    public String getWorldName() { return worldName; }
    public String getPluginDirectory() { return pluginDirectory; }
    public int getMaxTickTime() { return maxTickTime; }
    public boolean isAllowNether() { return allowNether; }
    public boolean isAllowEnd() { return allowEnd; }
    public String getLevelType() { return levelType; }
    public boolean isGenerateStructures() { return generateStructures; }
    public boolean isAllowFlight() { return allowFlight; }
    public boolean isPvp() { return pvp; }
    public boolean isSpawnAnimals() { return spawnAnimals; }
    public boolean isSpawnMonsters() { return spawnMonsters; }
    public int getMaxBuildHeight() { return maxBuildHeight; }
    public String getResourcePack() { return resourcePack; }
    public String getResourcePackSha1() { return resourcePackSha1; }
}
