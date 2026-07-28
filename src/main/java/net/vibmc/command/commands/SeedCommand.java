package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class SeedCommand implements Command {
    @Override
    public String getName() { return "seed"; }
    @Override
    public String getDescription() { return "Show the world seed"; }
    @Override
    public String getUsage() { return "/seed"; }
    @Override
    public String getPermission() { return "vibmc.command.seed"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long seed = VibMC.getInstance().getWorldManager().getMainWorld().getSeed();
        sender.sendMessage("{\"text\":\"§aSeed: §f" + seed + "\"}");
        return true;
    }
}
