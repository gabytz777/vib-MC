package net.vibmc.entity.ai;

import net.vibmc.entity.LivingEntity;

public class FollowTargetGoal implements Goal {
    private final LivingEntity entity;
    private LivingEntity target;
    private double followRange;
    private double attackRange;

    public FollowTargetGoal(LivingEntity entity, double followRange, double attackRange) {
        this.entity = entity;
        this.followRange = followRange;
        this.attackRange = attackRange;
    }

    @Override
    public boolean shouldStart() {
        if (target == null || !target.isAlive()) {
            target = findNearestTarget();
        }
        return target != null && entity.distanceTo(target) <= followRange;
    }

    private LivingEntity findNearestTarget() {
        LivingEntity nearest = null;
        double nearestDist = followRange;

        for (var other : entity.getWorld().getEntities()) {
            if (other == entity || !other.isAlive()) continue;
            if (other instanceof LivingEntity living) {
                double dist = entity.distanceTo(living);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = living;
                }
            }
        }
        return nearest;
    }

    @Override
    public void start() {
        if (target == null) return;
        entity.getPathfinding().findPath(
            entity.getWorld(),
            entity.getX(), entity.getY(), entity.getZ(),
            target.getX(), target.getY(), target.getZ(),
            300
        );
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            stop();
            return;
        }

        double dist = entity.distanceTo(target);
        if (dist <= attackRange) {
            attack();
        } else if (dist <= followRange) {
            double dx = target.getX() - entity.getX();
            double dz = target.getZ() - entity.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            entity.setMotionX((dx / d) * entity.getSpeed());
            entity.setMotionZ((dz / d) * entity.getSpeed());
        }
    }

    private void attack() {
        if (target == null) return;
        target.damage(entity.getAttackDamage());
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean shouldContinue() {
        return target != null && target.isAlive() && entity.distanceTo(target) <= followRange;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public LivingEntity getTarget() {
        return target;
    }
}
