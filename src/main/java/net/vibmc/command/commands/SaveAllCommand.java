package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class SaveAllCommand extends Command {
    public SaveAllCommand() {
        super("save-all", "Save the world", "/save-all", "vibmc.command.save");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int written = VibMC.getInstance().getWorldManager().saveAll();
        sender.sendMessage("{\"text\":\"§aWorld saved (" + written + " chunk" + (written == 1 ? "" : "s") + " written).\"}");
        return true;
    }
}
