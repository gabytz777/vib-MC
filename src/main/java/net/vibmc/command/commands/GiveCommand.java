package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.item.ItemRegistry;
import net.vibmc.item.ItemStack;
import net.vibmc.server.VibMC;

public class GiveCommand implements Command {
    @Override
    public String getName() { return "give"; }
    @Override
    public String getDescription() { return "Give an item to a player"; }
    @Override
    public String getUsage() { return "/give <player> <item> [amount]"; }
    @Override
    public String getPermission() { return "vibmc.command.give"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("{\"text\":\"§cUsage: " + getUsage() + "\"}");
            return true;
        }

        PlayerEntity target = VibMC.getInstance().getPlayerManager().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("{\"text\":\"§cPlayer not found: " + args[0] + "\"}");
            return true;
        }

        ItemStack item = ItemRegistry.createStack(args[1], 1);
        if (item == null) {
            sender.sendMessage("{\"text\":\"§cUnknown item: " + args[1] + "\"}");
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("{\"text\":\"§cInvalid amount\"}");
                return true;
            }
        }

        item = new ItemStack(item.getType(), amount);
        int remaining = target.getInventory().addItem(item);

        if (remaining > 0) {
            sender.sendMessage("{\"text\":\"§e" + target.getUsername() + "'s inventory is full. " + remaining + " items dropped.\"}");
        } else {
            sender.sendMessage("{\"text\":\"§aGave " + amount + " " + item.getType().getName() + " to " + target.getUsername() + "\"}");
        }
        return true;
    }
}
