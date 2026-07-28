package net.vibmc.plugin.event;

import net.vibmc.entity.PlayerEntity;

public class PlayerMoveEvent extends Event implements Cancellable {
    private final PlayerEntity player;
    private final double fromX, fromY, fromZ;
    private final double toX, toY, toZ;
    private boolean cancelled;

    public PlayerMoveEvent(PlayerEntity player, double fromX, double fromY, double fromZ,
                           double toX, double toY, double toZ) {
        this.player = player;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
        this.cancelled = false;
    }

    public PlayerEntity getPlayer() { return player; }
    public double getFromX() { return fromX; }
    public double getFromY() { return fromY; }
    public double getFromZ() { return fromZ; }
    public double getToX() { return toX; }
    public double getToY() { return toY; }
    public double getToZ() { return toZ; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
