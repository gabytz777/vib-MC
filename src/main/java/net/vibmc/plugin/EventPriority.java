package net.vibmc.plugin;

public enum EventPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4),
    MONITOR(5);

    private final int value;

    EventPriority(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
