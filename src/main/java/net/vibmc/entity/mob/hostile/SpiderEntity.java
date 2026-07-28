package net.vibmc.entity.mob.hostile;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.FollowTargetGoal;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class SpiderEntity extends LivingEntity {
    public SpiderEntity(World world) {
        super(world);
        this.maxHealth = 16.0f;
        this.health = 16.0f;
        this.speed = 0.2f;
        this.attackDamage = 2.0f;
        addGoal(new FollowTargetGoal(this, 16.0, 2.0));
        addGoal(new WanderGoal(this));
    }

    @Override
    protected void onDeath() {
        // Drop string and spider eye
    }
}
