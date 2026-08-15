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

public class WorldManager {
    private final Map<String, World> worlds = new LinkedHashMap<>();
    private final Map<Dimension, World> byDimension = new LinkedHashMap<>();
    private final World mainWorld;

    public WorldManager(ServerConfig config) {
        String levelName = config.worldName();

        // The overworld decides the seed for the whole server; the Nether and End are
        // generated from the same seed so a save is internally consistent.
        this.mainWorld = loadOrCreate(config, levelName, Dimension.OVERWORLD, null);
        long seed = mainWorld.seed();
        loadOrCreate(config, levelName, Dimension.NETHER, seed);
        loadOrCreate(config, levelName, Dimension.END, seed);
    }

    /**
     * Brings one dimension's world online, restoring its saved state when there is one.
     *
     * @param knownSeed the seed to use, or null to resolve it from config/level.dat
     */
    private World loadOrCreate(ServerConfig config, String levelName, Dimension dimension,
                               Long knownSeed) {
        String directory = levelName + dimension.folderSuffix();
        WorldStorage storage = new WorldStorage(directory);
        try {
            storage.prepare();
        } catch (IOException e) {
            warn("Could not create the world directory %s: %s", storage.worldDir(), e);
        }

        LevelData restored = null;
        if (storage.hasLevelData()) {
            try {
                restored = storage.readLevel();
            } catch (IOException e) {
                warn("Could not read %s (%s); starting this world from the configured seed",
                        storage.worldDir().resolve("level.dat"), e.getMessage());
            }
        }

        long seed;
        if (restored != null) {
            // A world that already exists on disk always keeps the seed it was
            // generated with, no matter what server.properties says now.
            seed = restored.seed();
            String setting = config.seedSetting();
            if (knownSeed == null && !Seeds.isBlank(setting) && Seeds.resolve(setting) != seed) {
                warn("server.properties has seed '%s' but the saved world was generated with %d; "
                        + "using the saved seed so existing terrain still lines up", setting, seed);
            }
        } else if (knownSeed != null) {
            seed = knownSeed;
        } else {
            // Blank rolls a random seed; numeric is used as-is; text is hashed. Either way
            // it is recorded in level.dat below, never written back to server.properties.
            seed = Seeds.resolve(config.seedSetting());
        }

        World world = new World(seed, directory, dimension, storage);
        worlds.put(world.name(), world);
        byDimension.put(dimension, world);

        if (restored != null) {
            world.setWorldTime(restored.worldTime());
            world.setTimeOfDay(restored.timeOfDay());
            world.weatherSystem().setWeather(restored.weather());
            info("Loaded world '%s' (%s, seed %d)", directory, dimension, seed);
        } else {
            // Write level.dat straight away so the seed this world was generated with is
            // recorded before any terrain exists, rather than only at the first save.
            try {
                storage.writeLevel(new LevelData(seed, 0L, world.getDayTime(), "clear"));
            } catch (IOException e) {
                warn("Could not write initial level data for '%s': %s", directory, e);
            }
            info("Created world '%s' (%s, seed %d)", directory, dimension, seed);
        }
        return world;
    }

    public World getMainWorld() {
        return mainWorld;
    }

    /** The world backing a given dimension, or null if it is not loaded. */
    public World getWorld(Dimension dimension) {
        return byDimension.get(dimension);
    }

    public World getNether() {
        return byDimension.get(Dimension.NETHER);
    }

    public World getEnd() {
        return byDimension.get(Dimension.END);
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
