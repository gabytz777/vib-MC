package net.vibmc.entity.ai;

public interface Goal {
    boolean shouldStart();
    void start();
    void tick();
    void stop();
    boolean shouldContinue();
}
