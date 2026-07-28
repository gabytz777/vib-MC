package net.vibmc.entity.mob.passive;

import net.vibmc.entity.LivingEntity;
import net.vibmc.entity.ai.WanderGoal;
import net.vibmc.world.World;

public class SheepEntity extends LivingEntity {
    private boolean sheared;

    public SheepEntity(World world) {
        super(world);
        this.maxHealth = 8.0f;
        this.health = 8.0f;
        this.speed = 0.1f;
        this.sheared = false;
        addGoal(new WanderGoal(this));
    }

    public boolean isSheared() { return sheared; }
    public void setSheared(boolean sheared) { this.sheared = sheared; }

    @Override
    protected void onDeath() {
        // Drop wool and mutton
    }
}
