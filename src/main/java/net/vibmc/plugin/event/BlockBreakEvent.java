package net.vibmc.plugin.event;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.util.Position;
import net.vibmc.world.block.Block;

public class BlockBreakEvent extends Event implements Cancellable {
    private final PlayerEntity player;
    private final Position position;
    private final Block block;
    private boolean cancelled;

    public BlockBreakEvent(PlayerEntity player, Position position, Block block) {
        this.player = player;
        this.position = position;
        this.block = block;
        this.cancelled = false;
    }

    public PlayerEntity getPlayer() { return player; }
    public Position getPosition() { return position; }
    public Block getBlock() { return block; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
