package net.vibmc.command;

import net.vibmc.entity.PlayerEntity;

public class CommandSender {
    private final Object source;

    public CommandSender(Object source) {
        this.source = source;
    }

    public boolean isPlayer() {
        return source instanceof PlayerEntity;
    }

    public boolean isConsole() {
        return source instanceof String && source.equals("CONSOLE");
    }

    public PlayerEntity getPlayer() {
        return isPlayer() ? (PlayerEntity) source : null;
    }

    public void sendMessage(String message) {
        if (isPlayer()) {
            getPlayer().sendMessage(message);
        } else {
            System.out.println(message.replaceAll("§[0-9a-fklmnor]", ""));
        }
    }

    public Object getSource() { return source; }
}
