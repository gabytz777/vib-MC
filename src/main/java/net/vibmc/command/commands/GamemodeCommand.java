package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.VibMC;

public class GamemodeCommand implements Command {
    @Override
    public String getName() { return "gamemode"; }
    @Override
    public String getDescription() { return "Change a player's game mode"; }
    @Override
    public String getUsage() { return "/gamemode <mode> [player]"; }
    @Override
    public String getPermission() { return "vibmc.command.gamemode"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        int mode;
        switch (args[0].toLowerCase()) {
            case "survival":
            case "0":
                mode = 0;
                break;
            case "creative":
            case "1":
                mode = 1;
                break;
            case "adventure":
            case "2":
                mode = 2;
                break;
            case "spectator":
            case "3":
                mode = 3;
                break;
            default:
                sender.sendMessage("{\"text\":\"§cInvalid gamemode. Use: survival, creative, adventure, spectator\"}");
                return true;
        }

        PlayerEntity target;
        if (args.length >= 2) {
            target = VibMC.getInstance().getPlayerManager().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("{\"text\":\"§cPlayer not found: " + args[1] + "\"}");
                return true;
            }
        } else if (sender.isPlayer()) {
            target = sender.getPlayer();
        } else {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        target.setGameMode(mode);
        String modeName = switch (mode) {
            case 0 -> "Survival";
            case 1 -> "Creative";
            case 2 -> "Adventure";
            case 3 -> "Spectator";
            default -> "Unknown";
        };
        String msg = "{\"text\":\"§aSet " + target.getUsername() + "'s game mode to " + modeName + "\"}";
        sender.sendMessage(msg);
        if (target != sender.getPlayer()) {
            target.sendMessage("{\"text\":\"§aYour game mode has been changed to " + modeName + "\"}");
        }
        return true;
    }
}
