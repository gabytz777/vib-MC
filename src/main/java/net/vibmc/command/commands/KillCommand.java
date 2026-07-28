package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.VibMC;

public class KillCommand implements Command {
    @Override
    public String getName() { return "kill"; }
    @Override
    public String getDescription() { return "Kill a player or entity"; }
    @Override
    public String getUsage() { return "/kill [player]"; }
    @Override
    public String getPermission() { return "vibmc.command.kill"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        PlayerEntity target;

        if (args.length >= 1) {
            target = VibMC.getInstance().getPlayerManager().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("{\"text\":\"§cPlayer not found: " + args[0] + "\"}");
                return true;
            }
        } else if (sender.isPlayer()) {
            target = sender.getPlayer();
        } else {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        target.die();
        sender.sendMessage("{\"text\":\"§cKilled " + target.getUsername() + "\"}");
        return true;
    }
}
