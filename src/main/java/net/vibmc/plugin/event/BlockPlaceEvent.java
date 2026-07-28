package net.vibmc.plugin.event;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.item.ItemStack;
import net.vibmc.server.util.Position;
import net.vibmc.world.block.Block;

public class BlockPlaceEvent extends Event implements Cancellable {
    private final PlayerEntity player;
    private final Position position;
    private final Block placed;
    private final Block replaced;
    private final ItemStack itemInHand;
    private boolean cancelled;

    public BlockPlaceEvent(PlayerEntity player, Position position, Block placed, Block replaced, ItemStack itemInHand) {
        this.player = player;
        this.position = position;
        this.placed = placed;
        this.replaced = replaced;
        this.itemInHand = itemInHand;
        this.cancelled = false;
    }

    public PlayerEntity getPlayer() { return player; }
    public Position getPosition() { return position; }
    public Block getPlaced() { return placed; }
    public Block getReplaced() { return replaced; }
    public ItemStack getItemInHand() { return itemInHand; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
