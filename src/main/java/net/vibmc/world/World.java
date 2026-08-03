package net.vibmc.world;

import net.vibmc.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class World {
    private final long seed;
    private final String name;
    private final ChunkManager chunkManager;
    private final TimeSystem timeSystem;
    private final WeatherSystem weatherSystem;
    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private long worldTime;

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

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunk(chunkX, chunkZ);
    }

    public void tick(long tick) {
        worldTime++;
        timeSystem.tick();
        if (tick % 100 == 0) {
            weatherSystem.tick();
        }
        for (Entity entity : new ArrayList<>(entities)) {
            entity.tick();
        }
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    public int getHighestBlockY(int x, int z) {
        Chunk chunk = chunkManager.getChunk(x >> 4, z >> 4);
        int localX = x & 15;
        int localZ = z & 15;
        for (int y = 255; y >= 0; y--) {
            if (chunk.getBlock(localX, y, localZ) != Block.AIR.id()) {
                return y;
            }
        }
        return 0;
    }

    public long seed() {
        return seed;
    }

    public String name() {
        return name;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public long getDayTime() {
        return timeSystem.timeOfDay();
    }

    public void setTimeOfDay(long time) {
        timeSystem.setTimeOfDay(time);
    }

    public void addTime(long ticks) {
        timeSystem.addTime(ticks);
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
