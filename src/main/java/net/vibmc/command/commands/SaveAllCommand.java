package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class SaveAllCommand implements Command {
    @Override
    public String getName() { return "save-all"; }
    @Override
    public String getDescription() { return "Save all worlds to disk"; }
    @Override
    public String getUsage() { return "/save-all"; }
    @Override
    public String getPermission() { return "vibmc.command.save"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        VibMC.getInstance().getLogger().info("Saving all worlds...");
        VibMC.getInstance().getWorldManager().saveAll();
        sender.sendMessage("{\"text\":\"§aWorlds saved.\"}");
        return true;
    }
}
