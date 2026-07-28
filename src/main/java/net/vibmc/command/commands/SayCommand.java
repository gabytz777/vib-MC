package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class SayCommand implements Command {
    @Override
    public String getName() { return "say"; }
    @Override
    public String getDescription() { return "Broadcast a message to all players"; }
    @Override
    public String getUsage() { return "/say <message>"; }
    @Override
    public String getPermission() { return "vibmc.command.say"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        String message = String.join(" ", args);
        String prefix = sender.isConsole() ? "§7[Server]" : "§7[" + sender.getPlayer().getUsername() + "]";
        VibMC.getInstance().getPlayerManager().broadcastMessage("{\"text\":\"" + prefix + " §f" + message + "\"}");
        return true;
    }
}
