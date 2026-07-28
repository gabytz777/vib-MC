package net.vibmc.entity.mob.hostile;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.FollowTargetGoal;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class ZombieEntity extends LivingEntity {
    public ZombieEntity(World world) {
        super(world);
        this.maxHealth = 20.0f;
        this.health = 20.0f;
        this.speed = 0.15f;
        this.attackDamage = 3.0f;
        addGoal(new FollowTargetGoal(this, 16.0, 2.0));
        addGoal(new WanderGoal(this));
    }

    @Override
    protected void onDeath() {
        // Drop rotten flesh
    }
}
