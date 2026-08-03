package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class TimeCommand extends Command {
    public TimeCommand() {
        super("time", "Set or add to the world time", "/time set <day|night|ticks> | /time add <ticks>", "vibmc.command.time");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("{\"text\":\"§cUsage: /time set <day|night|ticks> | /time add <ticks>\"}");
            return false;
        }
        var world = VibMC.getInstance().getWorldManager().getMainWorld();
        switch (args[0].toLowerCase()) {
            case "set": {
                long time;
                switch (args[1].toLowerCase()) {
                    case "day":
                        time = 1000;
                        break;
                    case "noon":
                        time = 6000;
                        break;
                    case "night":
                        time = 13000;
                        break;
                    case "midnight":
                        time = 18000;
                        break;
                    default:
                        try {
                            time = Long.parseLong(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("{\"text\":\"§cInvalid time value.\"}");
                            return false;
                        }
                        break;
                }
                world.setTimeOfDay(time);
                sender.sendMessage("{\"text\":\"§aTime set to " + time + ".\"}");
                return true;
            }
            case "add": {
                try {
                    long amount = Long.parseLong(args[1]);
                    world.addTime(amount);
                    sender.sendMessage("{\"text\":\"§aAdded " + amount + " ticks to the time.\"}");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("{\"text\":\"§cInvalid time value.\"}");
                    return false;
                }
            }
            default:
                sender.sendMessage("{\"text\":\"§cUsage: /time set <day|night|ticks> | /time add <ticks>\"}");
                return false;
        }
    }
}
