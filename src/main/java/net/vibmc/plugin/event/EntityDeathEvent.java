package net.vibmc.plugin.event;

import net.vibmc.entity.LivingEntity;
import net.vibmc.item.ItemStack;

import java.util.List;

public class EntityDeathEvent extends Event {
    private final LivingEntity entity;
    private final List<ItemStack> drops;

    public EntityDeathEvent(LivingEntity entity, List<ItemStack> drops) {
        this.entity = entity;
        this.drops = drops;
    }

    public LivingEntity getEntity() { return entity; }
    public List<ItemStack> getDrops() { return drops; }
}
