package net.vibmc.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerConfig {
    private final Properties properties;

    private ServerConfig(Properties properties) {
        this.properties = properties;
    }

    public static ServerConfig load(String path) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            props.load(in);
        } catch (IOException e) {
            // Missing or unreadable config; defaults will be used.
        }
        return new ServerConfig(props);
    }

    public String address() {
        return getString("server-ip", "0.0.0.0");
    }

    public int port() {
        return getInt("server-port", 25565);
    }

    public String worldName() {
        return getString("level-name", "world");
    }

    public long seed() {
        return getLong("seed", 0L);
    }

    public String motd() {
        return getString("motd", "A vib-MC Server");
    }

    public int maxPlayers() {
        return getInt("max-players", 20);
    }

    public int getMaxPlayers() {
        return maxPlayers();
    }

    public int getViewDistance() {
        return getInt("view-distance", 8);
    }

    public boolean onlineMode() {
        return getBoolean("online-mode", false);
    }

    public boolean allowFlight() {
        return getBoolean("allow-flight", false);
    }

    public String difficulty() {
        return getString("difficulty", "easy");
    }

    /** Ticks between automatic world saves. Zero or less disables autosaving. */
    public int autosaveIntervalTicks() {
        return getInt("autosave-interval-ticks", 6000);
    }

    /** Whether the world is written out when the server shuts down. */
    public boolean saveOnStop() {
        return getBoolean("save-on-stop", true);
    }

    private String getString(String key, String def) {
        return properties.getProperty(key, def);
    }

    private int getInt(String key, int def) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long getLong(String key, long def) {
        try {
            return Long.parseLong(properties.getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(def)).trim());
    }
}
