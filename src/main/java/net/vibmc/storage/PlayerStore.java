package net.vibmc.storage;

import net.vibmc.player.Player;
import net.vibmc.player.PlayerConnection;
import net.vibmc.player.PlayerManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PlayerStore {
    private final Path root;

    public PlayerStore(Path root) {
        this.root = root;
    }

    public void loadAll(PlayerManager manager) throws IOException {
        Files.createDirectories(root);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.properties")) {
            for (Path path : stream) {
                Properties properties = new Properties();
                try (InputStream in = Files.newInputStream(path)) {
                    properties.load(in);
                }
                String name = properties.getProperty("name");
                if (name != null && manager.get(name) == null) {
                    Player player = new Player(name);
                    player.setPosition(Double.parseDouble(properties.getProperty("x", "0")),
                            Double.parseDouble(properties.getProperty("y", "64")),
                            Double.parseDouble(properties.getProperty("z", "0")));
                    manager.add(new LoadedPlayerConnection(player));
                }
            }
        }
    }

    public void saveAll(PlayerManager manager) throws IOException {
        Files.createDirectories(root);
        for (PlayerConnection connection : manager.connections().values()) {
            savePlayer(connection.player());
        }
    }

    private void savePlayer(Player player) throws IOException {
        Path file = root.resolve(player.name().toLowerCase() + ".properties");
        Properties properties = new Properties();
        properties.setProperty("name", player.name());
        properties.setProperty("x", Double.toString(player.x()));
        properties.setProperty("y", Double.toString(player.y()));
        properties.setProperty("z", Double.toString(player.z()));
        properties.setProperty("health", Integer.toString(player.health()));
        properties.setProperty("hunger", Integer.toString(player.hunger()));
        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "vib-MC player save");
        }
    }

    private static final class LoadedPlayerConnection implements PlayerConnection {
        private final Player player;

        private LoadedPlayerConnection(Player player) {
            this.player = player;
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public void send(String message) {
            // Offline-loaded players have no live socket to send to.
        }

        @Override
        public void disconnect(String reason) {
            // Offline-loaded players are not connected.
        }
    }
}
