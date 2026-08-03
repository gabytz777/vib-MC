package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.item.ItemStack;
import net.vibmc.item.ItemType;
import net.vibmc.server.VibMC;

public class GiveCommand extends Command {
    public GiveCommand() {
        super("give", "Give an item to a player", "/give <player> <item> [amount]", "vibmc.command.give");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("{\"text\":\"§cUsage: /give <player> <item> [amount]\"}");
            return false;
        }
        PlayerEntity target = VibMC.getInstance().getPlayerManager().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("{\"text\":\"§cPlayer not found.\"}");
            return false;
        }
        ItemType type = ItemType.fromName(args[1]);
        if (type == null || type == ItemType.AIR) {
            sender.sendMessage("{\"text\":\"§cUnknown item: " + args[1] + "\"}");
            return false;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("{\"text\":\"§cInvalid amount.\"}");
                return false;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        target.addItem(new ItemStack(type, amount));
        sender.sendMessage("{\"text\":\"§aGave " + amount + " " + type.getName() + " to " + target.getUsername() + ".\"}");
        return true;
    }
}
