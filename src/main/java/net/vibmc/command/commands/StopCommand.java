package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class StopCommand extends Command {
    public StopCommand() {
        super("stop", "Shut down the server", "/stop", "vibmc.command.stop");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        VibMC.getInstance().stop();
        return true;
    }
}
