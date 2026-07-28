package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class StopCommand implements Command {
    @Override
    public String getName() { return "stop"; }
    @Override
    public String getDescription() { return "Stop the server"; }
    @Override
    public String getUsage() { return "/stop"; }
    @Override
    public String getPermission() { return "vibmc.command.stop"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        VibMC.getInstance().getLogger().info("Stopping server (issued by %s)...",
            sender.isConsole() ? "Console" : sender.getPlayer().getUsername());
        VibMC.getInstance().shutdown();
        return true;
    }
}
