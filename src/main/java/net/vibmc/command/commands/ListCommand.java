package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.server.VibMC;

public class ListCommand implements Command {
    @Override
    public String getName() { return "list"; }
    @Override
    public String getDescription() { return "List all online players"; }
    @Override
    public String getUsage() { return "/list"; }
    @Override
    public String getPermission() { return "vibmc.command.list"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var players = VibMC.getInstance().getPlayerManager().getOnlinePlayers();
        int count = players.size();
        int max = VibMC.getInstance().getConfig().getMaxPlayers();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"text\":\"§6There are §e").append(count).append("§6/§e").append(max)
          .append("§6 players online:\"}");
        sender.sendMessage(sb.toString());

        if (count > 0) {
            StringBuilder names = new StringBuilder("{\"text\":\"§f");
            boolean first = true;
            for (var player : players) {
                if (!first) names.append(", ");
                names.append(player.getUsername());
                first = false;
            }
            names.append("\"}");
            sender.sendMessage(names.toString());
        }
        return true;
    }
}
