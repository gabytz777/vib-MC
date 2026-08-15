package net.vibmc.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ServerConfig {
    private final Properties properties;

    private ServerConfig(Properties properties) {
        this.properties = properties;
    }

    public static ServerConfig load(String path) {
        Properties props = new Properties();
        try {
            byte[] raw = Files.readAllBytes(Paths.get(path));
            if (raw.length > 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF) {
                raw = Arrays.copyOfRange(raw, 3, raw.length);
            }
            props.load(new ByteArrayInputStream(raw));
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

    /**
     * {@code seed=0} (the default in a fresh server.properties) means "no seed was
     * chosen" rather than a literal seed of zero, so a brand-new world rolls a random
     * one instead of every fresh install generating the same terrain.
     */
    public boolean hasExplicitSeed() {
        return seed() != 0L;
    }

    /** Records the seed a new world was actually generated with, so future restarts see it. */
    public void setSeed(long seed) {
        properties.setProperty("seed", String.valueOf(seed));
        persist();
    }

    public int autosaveIntervalTicks() {
        return getInt("autosave-interval-ticks", 6000);
    }

    public boolean saveOnStop() {
        return getBoolean("save-on-stop", true);
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

    public String skinUrl() {
        return getString("skin-url", "").trim();
    }

    public String skinUrlFor(String username) {
        if (username != null) {
            String perPlayer = properties.getProperty("skin-url." + username.toLowerCase());
            if (perPlayer != null && !perPlayer.trim().isEmpty()) {
                return perPlayer.trim();
            }
        }
        return skinUrl();
    }

    public boolean hasSkinPluginSetting() {
        return properties.containsKey("skin-plugin-enabled");
    }

    public boolean skinPluginEnabled() {
        return getBoolean("skin-plugin-enabled", true);
    }

    public void enableSkinPlugin(boolean enable) {
        properties.setProperty("skin-plugin-enabled", String.valueOf(enable));
        persist();
    }

    public void setSkinUrlFor(String username, String url) {
        properties.setProperty("skin-url." + username.toLowerCase(), url);
        persist();
    }

    public void removeSkinUrlFor(String username) {
        properties.remove("skin-url." + username.toLowerCase());
        persist();
    }

    private void persist() {
        String path = "server.properties";
        try {
            List<String> lines = new ArrayList<>();
            String sep = System.lineSeparator();
            if (Files.exists(Paths.get(path))) {
                byte[] raw = Files.readAllBytes(Paths.get(path));
                String text = new String(raw, StandardCharsets.UTF_8);
                if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                    text = text.substring(1);
                }
                if (text.contains("\r\n")) {
                    sep = "\r\n";
                }
                lines.addAll(Arrays.asList(text.split("\\r?\\n", -1)));
            }
            for (String key : properties.stringPropertyNames()) {
                String prefix = key + "=";
                String value = properties.getProperty(key);
                boolean replaced = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith(prefix)) {
                        if (value.isEmpty()) {
                            lines.remove(i);
                        } else {
                            lines.set(i, prefix + value);
                        }
                        replaced = true;
                        break;
                    }
                }
                if (!replaced && !value.isEmpty()) {
                    lines.add(prefix + value);
                }
            }
            Files.write(Paths.get(path), String.join(sep, lines).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            VibMC.getInstance().getLogger().warn("Could not save server.properties: %s", e.getMessage());
        }
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
