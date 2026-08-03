package net.vibmc.plugin.event;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
