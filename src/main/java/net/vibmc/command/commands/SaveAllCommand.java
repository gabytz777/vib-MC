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
        int saved = VibMC.getInstance().getWorldManager().saveAll();
        if (saved == 0) {
            sender.sendMessage("{\"text\":\"§aWorld saved (nothing had changed).\"}");
        } else {
            sender.sendMessage("{\"text\":\"§aWorld saved, " + saved + " chunk(s) written.\"}");
        }
        return true;
    }
}
