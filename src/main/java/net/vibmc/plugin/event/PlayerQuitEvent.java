package net.vibmc.plugin.event;

import net.vibmc.entity.PlayerEntity;

public class PlayerQuitEvent extends Event {
    private final PlayerEntity player;
    private String quitMessage;

    public PlayerQuitEvent(PlayerEntity player, String quitMessage) {
        this.player = player;
        this.quitMessage = quitMessage;
    }

    public PlayerEntity getPlayer() { return player; }
    public String getQuitMessage() { return quitMessage; }
    public void setQuitMessage(String quitMessage) { this.quitMessage = quitMessage; }
}
