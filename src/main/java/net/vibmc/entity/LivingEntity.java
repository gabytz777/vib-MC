package net.vibmc.entity;

import net.vibmc.entity.ai.Goal;
import net.vibmc.entity.ai.Pathfinding;
import net.vibmc.server.util.Position;
import net.vibmc.world.World;

import java.util.ArrayList;
import java.util.List;

public abstract class LivingEntity extends Entity {
    protected double motionX, motionY, motionZ;
    protected float speed;
    protected float attackDamage;
    protected boolean aiEnabled;
    protected final List<Goal> goals;
    protected Pathfinding pathfinding;
    protected int age;
    protected int maxAge = 72000;

    public LivingEntity(World world) {
        super(world);
        this.speed = 0.1f;
        this.attackDamage = 1.0f;
        this.aiEnabled = true;
        this.goals = new ArrayList<>();
        this.pathfinding = new Pathfinding();
        this.age = 0;
    }

    @Override
    public void tick() {
        if (!alive) return;
        age++;
        if (age > maxAge) {
            remove();
            return;
        }
        if (aiEnabled) {
            updateAI();
        }
        applyPhysics();
    }

    protected void updateAI() {
        for (Goal goal : goals) {
            if (goal.shouldStart()) {
                goal.tick();
                break;
            }
        }
    }

    protected void applyPhysics() {
        if (!onGround) {
            motionY -= 0.08; // gravity
        }
        motionX *= 0.91;
        motionZ *= 0.91;
        if (onGround) {
            motionX *= 0.6;
            motionZ *= 0.6;
        }

        x += motionX;
        y += motionY;
        z += motionZ;

        if (motionY < -0.5 && !onGround) {
            // fall damage
            handleFallDamage(-motionY);
        }
    }

    protected void handleFallDamage(double fallDistance) {
        if (fallDistance > 3.0) {
            float damage = (float) (fallDistance - 3.0);
            damage(damage);
        }
    }

    public double getMotionX() { return motionX; }
    public double getMotionY() { return motionY; }
    public double getMotionZ() { return motionZ; }
    public void setMotionX(double motionX) { this.motionX = motionX; }
    public void setMotionY(double motionY) { this.motionY = motionY; }
    public void setMotionZ(double motionZ) { this.motionZ = motionZ; }
    public void addVelocity(double dx, double dy, double dz) {
        this.motionX += dx;
        this.motionY += dy;
        this.motionZ += dz;
    }

    public void knockback(double dx, double dz) {
        motionX += dx;
        motionZ += dz;
    }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public float getAttackDamage() { return attackDamage; }
    public void setAttackDamage(float damage) { this.attackDamage = damage; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }

    public void addGoal(Goal goal) {
        goals.add(goal);
    }

    public Pathfinding getPathfinding() { return pathfinding; }

    public double distanceTo(Entity other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
