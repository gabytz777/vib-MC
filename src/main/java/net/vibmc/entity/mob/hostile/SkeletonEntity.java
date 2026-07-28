package net.vibmc.entity.mob.hostile;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.FollowTargetGoal;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class SkeletonEntity extends LivingEntity {
    public SkeletonEntity(World world) {
        super(world);
        this.maxHealth = 20.0f;
        this.health = 20.0f;
        this.speed = 0.15f;
        this.attackDamage = 2.0f;
        addGoal(new FollowTargetGoal(this, 16.0, 8.0));
        addGoal(new WanderGoal(this));
    }

    @Override
    protected void onDeath() {
        // Drop bones and arrows
    }
}
