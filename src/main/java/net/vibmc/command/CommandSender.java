package net.vibmc.command;

import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.VibMC;

public class CommandSender {
    private final String name;
    private final PlayerEntity player;

    public CommandSender(String name) {
        this.name = name;
        this.player = null;
    }

    public CommandSender(PlayerEntity player) {
        this.player = player;
        this.name = player.getUsername();
    }

    public boolean isPlayer() {
        return player != null;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public void sendMessage(String message) {
        if (player != null) {
            player.sendMessage(message);
        } else {
            VibMC.getInstance().getLogger().info(message);
        }
    }
}
