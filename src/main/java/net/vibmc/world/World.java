package net.vibmc.world;

import net.vibmc.entity.Entity;
import net.vibmc.world.storage.WorldStorage;

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
    private final WorldStorage storage;
    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private long worldTime;

    public World(long seed, String name) {
        this(seed, name, new WorldStorage(name));
    }

    public World(long seed, String name, WorldStorage storage) {
        this.seed = seed;
        this.name = name;
        this.storage = storage;
        this.timeSystem = new TimeSystem();
        this.weatherSystem = new WeatherSystem();
        this.chunkManager = new ChunkManager(this, storage);
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
        Chunk chunk = chunkManager.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        for (int y = 255; y >= 0; y--) {
            if (chunk.getBlock(localX, y, localZ) != Block.AIR.id()) {
                return y;
            }
        }
        return 0;
    }

    public int getHighestSolidY(int x, int z) {
        Chunk chunk = chunkManager.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        for (int y = 255; y >= 0; y--) {
            short id = chunk.getBlock(localX, y, localZ);
            if (id != Block.AIR.id() && id != Block.WATER.id() && id != Block.LAVA.id()) {
                return y;
            }
        }
        return 0;
    }

    /** Finds the nearest dry-land spawn column within radius, starting from (x, z). */
    public int[] findDrySpawn(int x, int z, int radius) {
        int best = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int wx = x + dx;
                int wz = z + dz;
                if (getHighestSolidY(wx, wz) >= getSeaLevel() && getHighestBlockY(wx, wz) < 256) {
                    int dist = dx * dx + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = (wx << 16) | (wz & 0xFFFF);
                    }
                }
            }
        }
        if (best < 0) return new int[]{x, z};
        int sx = best >> 16;
        int sz = (short) (best & 0xFFFF);
        return new int[]{sx, sz};
    }

    public int getSeaLevel() {
        return 13;
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

    public void setWorldTime(long worldTime) {
        this.worldTime = worldTime;
    }

    public WorldStorage storage() {
        return storage;
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
