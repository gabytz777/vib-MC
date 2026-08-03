package net.vibmc.world;

public class TimeSystem {
    private long timeOfDay;

    public TimeSystem() {
        this.timeOfDay = 6000;
    }

    public void tick() {
        timeOfDay = (timeOfDay + 1) % 24000;
    }

    public void setTimeOfDay(long time) {
        this.timeOfDay = ((time % 24000) + 24000) % 24000;
    }

    public void addTime(long ticks) {
        setTimeOfDay(timeOfDay + ticks);
    }

    public String phase() {
        if (timeOfDay < 6000) {
            return "day";
        }
        if (timeOfDay < 18000) {
            return "sunset";
        }
        return "night";
    }

    public long timeOfDay() {
        return timeOfDay;
    }
}
