package net.vibmc.command;

import net.vibmc.entity.PlayerEntity;

public interface Command {
    String getName();
    String getDescription();
    String getUsage();
    String getPermission();
    boolean execute(CommandSender sender, String[] args);
}
