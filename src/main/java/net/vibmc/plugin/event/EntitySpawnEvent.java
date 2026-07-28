package net.vibmc.plugin.event;

import net.vibmc.entity.Entity;

public class EntitySpawnEvent extends Event implements Cancellable {
    private final Entity entity;
    private boolean cancelled;

    public EntitySpawnEvent(Entity entity) {
        this.entity = entity;
        this.cancelled = false;
    }

    public Entity getEntity() { return entity; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
