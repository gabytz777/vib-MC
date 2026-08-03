package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.VibMC;

public class KillCommand extends Command {
    public KillCommand() {
        super("kill", "Kill yourself or another player", "/kill [player]", null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        PlayerEntity target;
        if (args.length >= 1) {
            target = VibMC.getInstance().getPlayerManager().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("{\"text\":\"§cPlayer not found.\"}");
                return false;
            }
        } else if (sender.isPlayer()) {
            target = sender.getPlayer();
        } else {
            sender.sendMessage("{\"text\":\"§cUsage: /kill [player]\"}");
            return false;
        }
        target.die();
        sender.sendMessage("{\"text\":\"§aKilled " + target.getUsername() + ".\"}");
        return true;
    }
}
