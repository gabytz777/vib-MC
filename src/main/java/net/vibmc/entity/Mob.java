package net.vibmc.entity;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Mob implements Entity {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();
    private final int id = ID_GENERATOR.incrementAndGet();
    private final String type;
    private int ticksAlive;
    private boolean dead;

    protected Mob(String type) {
        this.type = type;
    }

    @Override
    public void tick() {
        ticksAlive++;
        if (ticksAlive > 200 && Math.random() < 0.01) {
            dead = true;
        }
    }

    @Override
    public boolean isDead() {
        return dead;
    }

    public int id() {
        return id;
    }

    public String type() {
        return type;
    }
}
