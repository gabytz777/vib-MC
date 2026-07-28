package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class WeatherCommand implements Command {
    @Override
    public String getName() { return "weather"; }
    @Override
    public String getDescription() { return "Change the weather"; }
    @Override
    public String getUsage() { return "/weather <clear|rain|thunder> [duration]"; }
    @Override
    public String getPermission() { return "vibmc.command.weather"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        var world = VibMC.getInstance().getWorldManager().getMainWorld();
        int duration = args.length >= 2 ? Integer.parseInt(args[1]) : -1;

        switch (args[0].toLowerCase()) {
            case "clear" -> {
                world.setRaining(false);
                world.setThundering(false);
                if (duration > 0) world.setClearWeatherTime(duration);
                sender.sendMessage("{\"text\":\"§aSet weather to clear\"}");
            }
            case "rain" -> {
                world.setRaining(true);
                world.setThundering(false);
                if (duration > 0) world.setRainTime(duration);
                sender.sendMessage("{\"text\":\"§aSet weather to rain\"}");
            }
            case "thunder" -> {
                world.setRaining(true);
                world.setThundering(true);
                if (duration > 0) world.setThunderTime(duration);
                sender.sendMessage("{\"text\":\"§aSet weather to thunder\"}");
            }
            default -> sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
        }
        return true;
    }
}
