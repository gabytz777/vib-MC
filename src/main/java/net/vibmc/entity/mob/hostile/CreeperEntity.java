package net.vibmc.entity.mob.hostile;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.FollowTargetGoal;
import net.vibmc.world.World;

public class CreeperEntity extends LivingEntity {
    private int fuseTime;
    private boolean ignited;
    private int explosionRadius;

    public CreeperEntity(World world) {
        super(world);
        this.maxHealth = 20.0f;
        this.health = 20.0f;
        this.speed = 0.15f;
        this.fuseTime = 30;
        this.explosionRadius = 3;
        this.ignited = false;
        addGoal(new FollowTargetGoal(this, 16.0, 3.0));
    }

    public void ignite() {
        this.ignited = true;
        this.fuseTime = 30;
    }

    @Override
    public void tick() {
        super.tick();
        if (ignited) {
            fuseTime--;
            if (fuseTime <= 0) {
                explode();
            }
        }
    }

    private void explode() {
        if (!alive) return;
        alive = false;

        // Damage nearby entities
        for (var entity : world.getEntities()) {
            if (entity == this) continue;
            double dist = distanceTo(entity);
            if (dist <= explosionRadius) {
                float damage = (float) (explosionRadius - dist) * 2.0f;
                entity.damage(damage);
            }
        }

        // Destroy blocks
        onDeath();
    }

    @Override
    protected void onDeath() {
        // Drop gunpowder
    }
}
