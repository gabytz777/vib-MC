package net.vibmc.world;

import net.vibmc.server.ServerConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorldManager {
    private final Map<String, World> worlds = new LinkedHashMap<>();
    private final World mainWorld;

    public WorldManager(ServerConfig config) {
        this.mainWorld = new World(config.seed(), config.worldName());
        worlds.put(mainWorld.name(), mainWorld);
    }

    public World getMainWorld() {
        return mainWorld;
    }

    public World getWorld(String name) {
        return worlds.get(name);
    }

    public void addWorld(World world) {
        worlds.put(world.name(), world);
    }

    public Collection<World> getWorlds() {
        return Collections.unmodifiableCollection(worlds.values());
    }
}
