package net.vibmc.entity;

public interface Entity {
    void tick();

    default boolean isDead() {
        return false;
    }
}
