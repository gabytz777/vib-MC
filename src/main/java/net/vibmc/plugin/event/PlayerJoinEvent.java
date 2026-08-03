package net.vibmc.plugin.event;

import net.vibmc.entity.PlayerEntity;

public class PlayerJoinEvent extends Event {
    private final PlayerEntity player;
    private String joinMessage;

    public PlayerJoinEvent(PlayerEntity player, String joinMessage) {
        this.player = player;
        this.joinMessage = joinMessage;
    }

    public PlayerEntity getPlayer() { return player; }
    public String getJoinMessage() { return joinMessage; }
    public void setJoinMessage(String joinMessage) { this.joinMessage = joinMessage; }
}
