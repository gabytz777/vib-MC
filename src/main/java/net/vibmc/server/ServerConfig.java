package net.vibmc.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record ServerConfig(
        String motd,
        int maxPlayers,
        long seed,
        String worldName,
        String difficulty,
        int viewDistance,
        int simulationDistance,
        boolean onlineMode,
        int port
) {
    public static ServerConfig load(String resourceName) throws IOException {
        Properties properties = new Properties();
        File file = new File(resourceName);
        try (InputStream inputStream = file.exists() ? new FileInputStream(file) : Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Unable to locate " + resourceName);
            }
            properties.load(inputStream);
        }

        return new ServerConfig(
                properties.getProperty("motd", "Welcome to vib-MC"),
                Integer.parseInt(properties.getProperty("max-players", "20")),
                Long.parseLong(properties.getProperty("seed", "1337")),
                properties.getProperty("world-name", "world"),
                properties.getProperty("difficulty", "normal"),
                Integer.parseInt(properties.getProperty("view-distance", "8")),
                Integer.parseInt(properties.getProperty("simulation-distance", "8")),
                Boolean.parseBoolean(properties.getProperty("online-mode", "false")),
                Integer.parseInt(properties.getProperty("port", "25565"))
        );
    }
}
