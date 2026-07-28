package net.vibmc.entity.ai;

import net.vibmc.entity.LivingEntity;
import net.vibmc.server.util.Position;

import java.util.Random;

public class WanderGoal implements Goal {
    private final LivingEntity entity;
    private final Random random;
    private Position target;
    private int cooldown;

    public WanderGoal(LivingEntity entity) {
        this.entity = entity;
        this.random = new Random();
        this.cooldown = 0;
    }

    @Override
    public boolean shouldStart() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return random.nextFloat() < 0.02f;
    }

    @Override
    public void start() {
        double tx = entity.getX() + (random.nextDouble() - 0.5) * 10;
        double tz = entity.getZ() + (random.nextDouble() - 0.5) * 10;
        double ty = entity.getY();
        this.target = new Position((int) tx, (int) ty, (int) tz);
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.5) {
            entity.setMotionX((dx / dist) * entity.getSpeed());
            entity.setMotionZ((dz / dist) * entity.getSpeed());
            entity.setRotation((float) Math.toDegrees(Math.atan2(-dx, dz)), 0);
        }
    }

    @Override
    public void stop() {
        target = null;
        cooldown = 100;
    }

    @Override
    public boolean shouldContinue() {
        if (target == null) return false;
        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        return Math.sqrt(dx * dx + dz * dz) > 1.0;
    }
}
