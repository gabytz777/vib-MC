package net.vibmc.player;

import net.vibmc.server.Server;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerManager {
    private final Server server;
    private final Map<String, PlayerConnection> connections = new LinkedHashMap<>();

    public PlayerManager(Server server) {
        this.server = server;
    }

    public void add(PlayerConnection connection) {
        if (connection == null || connection.player() == null) {
            return;
        }
        connections.put(connection.player().name().toLowerCase(), connection);
        connection.send("WELCOME:" + connection.player().name());
    }

    public void remove(String name) {
        if (name == null) {
            return;
        }
        connections.remove(name.toLowerCase());
    }

    public PlayerConnection get(String name) {
        if (name == null) {
            return null;
        }
        return connections.get(name.toLowerCase());
    }

    public void broadcast(String message) {
        for (PlayerConnection connection : connections.values()) {
            connection.send("CHAT:" + message);
        }
    }

    public Map<String, PlayerConnection> connections() {
        return Collections.unmodifiableMap(connections);
    }

    public String playerList() {
        return connections.values().stream()
                .map(connection -> connection.player().name())
                .collect(Collectors.joining(", "));
    }
}
