package net.vibmc.entity.mob.passive;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class ChickenEntity extends LivingEntity {
    private int eggLayTime;

    public ChickenEntity(World world) {
        super(world);
        this.maxHealth = 4.0f;
        this.health = 4.0f;
        this.speed = 0.15f;
        this.eggLayTime = 6000 + (int)(Math.random() * 6000);
        addGoal(new WanderGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (--eggLayTime <= 0) {
            layEgg();
            eggLayTime = 6000 + (int)(Math.random() * 6000);
        }
    }

    private void layEgg() {
        // Drop egg item
    }

    @Override
    protected void onDeath() {
        // Drop feather and chicken
    }
}
