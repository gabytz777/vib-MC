package net.vibmc.world.storage;

/**
 * World-level state persisted in {@code level.dat}: everything needed to bring a
 * world back exactly as it was, apart from the block data itself.
 */
public class LevelData {
    private final long seed;
    private final long worldTime;
    private final long timeOfDay;
    private final String weather;

    public LevelData(long seed, long worldTime, long timeOfDay, String weather) {
        this.seed = seed;
        this.worldTime = worldTime;
        this.timeOfDay = timeOfDay;
        this.weather = weather == null ? "clear" : weather;
    }

    public long seed() {
        return seed;
    }

    public long worldTime() {
        return worldTime;
    }

    public long timeOfDay() {
        return timeOfDay;
    }

    public String weather() {
        return weather;
    }
}
