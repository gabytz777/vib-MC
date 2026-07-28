package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class TimeCommand implements Command {
    @Override
    public String getName() { return "time"; }
    @Override
    public String getDescription() { return "Change or query the world time"; }
    @Override
    public String getUsage() { return "/time <set|add|query> <value>"; }
    @Override
    public String getPermission() { return "vibmc.command.time"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        var world = VibMC.getInstance().getWorldManager().getMainWorld();

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage("{\"text\":\"§cUsage: /time set <value|day|night|noon|midnight>\"}");
                    return true;
                }
                long time;
                switch (args[1].toLowerCase()) {
                    case "day" -> time = 1000;
                    case "night" -> time = 13000;
                    case "noon" -> time = 6000;
                    case "midnight" -> time = 18000;
                    default -> {
                        try { time = Long.parseLong(args[1]); }
                        catch (NumberFormatException e) {
                            sender.sendMessage("{\"text\":\"§cInvalid time value\"}");
                            return true;
                        }
                    }
                }
                world.setWorldTime(time);
                sender.sendMessage("{\"text\":\"§aSet time to " + time + "\"}");
            }
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("{\"text\":\"§cUsage: /time add <value>\"}");
                    return true;
                }
                try {
                    long add = Long.parseLong(args[1]);
                    world.setWorldTime(world.getWorldTime() + add);
                    sender.sendMessage("{\"text\":\"§aAdded " + add + " to time\"}");
                } catch (NumberFormatException e) {
                    sender.sendMessage("{\"text\":\"§cInvalid time value\"}");
                }
            }
            case "query" -> {
                sender.sendMessage("{\"text\":\"§aThe current time is " + world.getDayTime() + "\"}");
            }
            default -> sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
        }
        return true;
    }
}
