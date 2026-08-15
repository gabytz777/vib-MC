package net.vibmc.command.commands;

import net.vibmc.command.Command;
import net.vibmc.command.CommandSender;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.player.PlayerManager;
import net.vibmc.server.ServerConfig;
import net.vibmc.server.VibMC;

public class SkinCommand extends Command {
    public SkinCommand() {
        super("skin", "Set a custom skin for a player", "/skin set <url> [player] | /skin remove [player] | /skin info [player]", "vibmc.command.skin");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        VibMC server = VibMC.getInstance();
        ServerConfig config = server.getConfig();
        if (!config.skinPluginEnabled()) {
            sender.sendMessage("{\"text\":\"§cThe skins plugin is disabled on this server.\"}");
            return false;
        }
        if (args.length < 1) {
            sender.sendMessage("{\"text\":\"§cUsage: /skin set <url> [player] | /skin remove [player] | /skin info [player]\"}");
            return false;
        }
        String action = args[0].toLowerCase();
        if (action.equals("set")) {
            if (args.length < 2) {
                sender.sendMessage("{\"text\":\"§cUsage: /skin set <url> [player]\"}");
                return false;
            }
            String url = args[1].trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                sender.sendMessage("{\"text\":\"§cInvalid skin url. Use a direct http(s) link to a PNG image.\"}");
                return false;
            }
            String targetName = targetName(sender, args, 2);
            if (targetName == null) {
                sender.sendMessage("{\"text\":\"§cUsage: /skin set <url> [player]\"}");
                return false;
            }
            config.setSkinUrlFor(targetName, url);
            refreshIfOnline(targetName);
            server.getLogger().info("Skin for %s set to %s", targetName, url);
            broadcastSkin("§a" + targetName + "'s skin was updated.");
            sender.sendMessage("{\"text\":\"§aSkin set for " + targetName + ". "
                    + "Note: the url must be a direct PNG (textures.minecraft.net links work best).\"}");
            return true;
        }
        if (action.equals("remove")) {
            String targetName = targetName(sender, args, 1);
            if (targetName == null) {
                sender.sendMessage("{\"text\":\"§cUsage: /skin remove [player]\"}");
                return false;
            }
            config.removeSkinUrlFor(targetName);
            refreshIfOnline(targetName);
            server.getLogger().info("Skin for %s removed", targetName);
            broadcastSkin("§a" + targetName + "'s skin was reset.");
            sender.sendMessage("{\"text\":\"§aRemoved " + targetName + "'s skin.\"}");
            return true;
        }
        if (action.equals("info")) {
            String targetName = targetName(sender, args, 1);
            if (targetName == null) {
                sender.sendMessage("{\"text\":\"§cUsage: /skin info [player]\"}");
                return false;
            }
            String url = config.skinUrlFor(targetName);
            if (url.isEmpty()) {
                sender.sendMessage("{\"text\":\"§7" + targetName + " has no skin set (default).\"}");
            } else {
                sender.sendMessage("{\"text\":\"§7" + targetName + "'s skin: " + url + "\"}");
            }
            return true;
        }
        sender.sendMessage("{\"text\":\"§cUsage: /skin set <url> [player] | /skin remove [player] | /skin info [player]\"}");
        return false;
    }

    private String targetName(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            return args[index];
        }
        if (sender.isPlayer()) {
            return sender.getPlayer().getUsername();
        }
        return null;
    }

    private void refreshIfOnline(String username) {
        PlayerEntity online = VibMC.getInstance().getPlayerManager().getPlayer(username);
        if (online != null) {
            VibMC.getInstance().getPlayerManager().refreshSkin(online);
        }
    }

    private void broadcastSkin(String message) {
        PlayerManager playerManager = VibMC.getInstance().getPlayerManager();
        playerManager.broadcastMessage("{\"text\":\"" + message + "\"}");
    }
}