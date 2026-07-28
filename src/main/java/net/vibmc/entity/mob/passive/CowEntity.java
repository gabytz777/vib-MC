package net.vibmc.entity.mob.passive;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class CowEntity extends LivingEntity {
    public CowEntity(World world) {
        super(world);
        this.maxHealth = 10.0f;
        this.health = 10.0f;
        this.speed = 0.1f;
        addGoal(new WanderGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void onDeath() {
        // Drop leather and beef
    }
}
