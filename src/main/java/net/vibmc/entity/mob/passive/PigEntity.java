package net.vibmc.entity.mob.passive;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class PigEntity extends LivingEntity {
    public PigEntity(World world) {
        super(world);
        this.maxHealth = 10.0f;
        this.health = 10.0f;
        this.speed = 0.12f;
        addGoal(new WanderGoal(this));
    }

    @Override
    protected void onDeath() {
        // Drop porkchop
    }
}
