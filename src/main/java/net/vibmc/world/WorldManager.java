package net.vibmc.world;

import net.vibmc.server.ServerConfig;
import net.vibmc.server.VibMC;
import net.vibmc.server.util.Logger;
import net.vibmc.world.storage.LevelData;
import net.vibmc.world.storage.WorldStorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class WorldManager {
    private final Map<String, World> worlds = new LinkedHashMap<>();
    private final World mainWorld;

    public WorldManager(ServerConfig config) {
        String levelName = config.worldName();
        WorldStorage storage = new WorldStorage(levelName);

        try {
            storage.prepare();
        } catch (IOException e) {
            warn("Could not create the world directory %s: %s", storage.worldDir(), e);
        }

        long seed;
        LevelData restored = null;
        if (storage.hasLevelData()) {
            try {
                restored = storage.readLevel();
            } catch (IOException e) {
                warn("Could not read %s (%s); starting this world from the configured seed",
                        storage.worldDir().resolve("level.dat"), e.getMessage());
            }
        }

        if (restored != null) {
            // A world that already exists on disk always keeps the seed it was
            // generated with, no matter what server.properties says now.
            seed = restored.seed();
            if (config.hasExplicitSeed() && config.seed() != seed) {
                warn("server.properties has seed %d but the saved world was generated with %d; "
                        + "using the saved seed so existing terrain still lines up", config.seed(), seed);
            }
        } else if (config.hasExplicitSeed()) {
            seed = config.seed();
        } else {
            // No seed configured yet: roll a random one instead of every fresh
            // install generating identical terrain, and record it for next time.
            seed = new Random().nextLong();
            config.setSeed(seed);
        }

        this.mainWorld = new World(seed, levelName, storage);
        worlds.put(mainWorld.name(), mainWorld);

        if (restored != null) {
            mainWorld.setWorldTime(restored.worldTime());
            mainWorld.setTimeOfDay(restored.timeOfDay());
            mainWorld.weatherSystem().setWeather(restored.weather());
            info("Loaded world '%s' (seed %d, time %d)", levelName, seed, restored.timeOfDay());
        } else {
            // Write level.dat straight away so the seed this world was generated with is
            // recorded before any terrain exists, rather than only at the first save.
            try {
                storage.writeLevel(new LevelData(seed, 0L, mainWorld.getDayTime(), "clear"));
            } catch (IOException e) {
                warn("Could not write initial level data for '%s': %s", levelName, e);
            }
            info("Created world '%s' (seed %d) in %s", levelName, seed, storage.worldDir().toAbsolutePath());
        }
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

    /**
     * Persists every world: level metadata first, then any chunk with unsaved changes.
     *
     * @return the total number of chunks written
     */
    public int saveAll() {
        int written = 0;
        for (World world : worlds.values()) {
            WorldStorage storage = world.storage();
            try {
                storage.writeLevel(new LevelData(
                        world.seed(),
                        world.getWorldTime(),
                        world.getDayTime(),
                        world.weatherSystem().weather()));
            } catch (IOException e) {
                warn("Failed to save level data for '%s': %s", world.name(), e);
            }
            written += world.chunkManager().saveAll();
        }
        return written;
    }

    /** Total number of loaded chunks across all worlds with changes not yet on disk. */
    public int getUnsavedChunkCount() {
        int dirty = 0;
        for (World world : worlds.values()) {
            dirty += world.chunkManager().getUnsavedChunkCount();
        }
        return dirty;
    }

    private static void info(String message, Object... args) {
        Logger logger = logger();
        if (logger != null) {
            logger.info(message, args);
        }
    }

    private static void warn(String message, Object... args) {
        Logger logger = logger();
        if (logger != null) {
            logger.warn(message, args);
        }
    }

    private static Logger logger() {
        VibMC server = VibMC.getInstance();
        return server == null ? null : server.getLogger();
    }
}
