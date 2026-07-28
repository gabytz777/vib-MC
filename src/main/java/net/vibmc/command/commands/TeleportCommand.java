package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.server.VibMC;

public class TeleportCommand implements Command {
    @Override
    public String getName() { return "tp"; }
    @Override
    public String getDescription() { return "Teleport a player"; }
    @Override
    public String getUsage() { return "/tp <player> [<x> <y> <z>] or /tp <player> <target>"; }
    @Override
    public String getPermission() { return "vibmc.command.tp"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        PlayerEntity target = VibMC.getInstance().getPlayerManager().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("{\"text\":\"§cPlayer not found: " + args[0] + "\"}");
            return true;
        }

        if (args.length == 4) {
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                target.setPosition(x, y, z);
                sender.sendMessage("{\"text\":\"§aTeleported " + target.getUsername() + " to " + x + ", " + y + ", " + z + "\"}");
            } catch (NumberFormatException e) {
                sender.sendMessage("{\"text\":\"§cInvalid coordinates\"}");
            }
        } else if (args.length == 2) {
            PlayerEntity destination = VibMC.getInstance().getPlayerManager().getPlayer(args[1]);
            if (destination == null) {
                sender.sendMessage("{\"text\":\"§cPlayer not found: " + args[1] + "\"}");
                return true;
            }
            target.setPosition(destination.getX(), destination.getY(), destination.getZ());
            target.setRotation(destination.getYaw(), destination.getPitch());
            sender.sendMessage("{\"text\":\"§aTeleported " + target.getUsername() + " to " + destination.getUsername() + "\"}");
        } else if (args.length == 1 && sender.isPlayer()) {
            // Teleport self to target is handled by the 2-arg case
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
        }

        return true;
    }
}
