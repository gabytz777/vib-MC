package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class HelpCommand implements Command {
    @Override
    public String getName() { return "help"; }
    @Override
    public String getDescription() { return "Shows a list of commands"; }
    @Override
    public String getUsage() { return "/help [command]"; }
    @Override
    public String getPermission() { return "vibmc.command.help"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            Command cmd = VibMC.getInstance().getCommandManager().getCommand(args[0]);
            if (cmd == null) {
                sender.sendMessage("{\"text\":\"§cUnknown command: " + args[0] + "\"}");
                return true;
            }
            sender.sendMessage("{\"text\":\"§6--- Help: " + cmd.getName() + " ---\"}");
            sender.sendMessage("{\"text\":\"§eDescription: §f" + cmd.getDescription() + "\"}");
            sender.sendMessage("{\"text\":\"§eUsage: §f" + cmd.getUsage() + "\"}");
            return true;
        }

        sender.sendMessage("{\"text\":\"§6--- Available Commands ---\"}");
        for (Command cmd : VibMC.getInstance().getCommandManager().getCommands().values()) {
            sender.sendMessage("{\"text\":\"§e/" + cmd.getName() + " §7- §f" + cmd.getDescription() + "\"}");
        }
        return true;
    }
}
