package net.vibmc.world;

import java.util.LinkedHashMap;
import java.util.Map;

public class World {
    private final long seed;
    private final String name;
    private final ChunkManager chunkManager;
    private final TimeSystem timeSystem;
    private final WeatherSystem weatherSystem;

    public World(long seed, String name) {
        this.seed = seed;
        this.name = name;
        this.timeSystem = new TimeSystem();
        this.weatherSystem = new WeatherSystem();
        this.chunkManager = new ChunkManager(this);
    }

    public Chunk chunk(int chunkX, int chunkZ) {
        return chunkManager.getChunk(chunkX, chunkZ);
    }

    public void tick(long tick) {
        timeSystem.tick();
        if (tick % 100 == 0) {
            weatherSystem.tick();
        }
        chunkManager.tick(tick);
    }

    public long seed() {
        return seed;
    }

    public String name() {
        return name;
    }

    public TimeSystem timeSystem() {
        return timeSystem;
    }

    public WeatherSystem weatherSystem() {
        return weatherSystem;
    }

    public ChunkManager chunkManager() {
        return chunkManager;
    }
}
