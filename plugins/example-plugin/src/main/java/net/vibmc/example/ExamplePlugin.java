package net.vibmc.example;

import net.vibmc.plugin.VibMCPlugin;
import net.vibmc.plugin.Listener;
import net.vibmc.plugin.event.*;
import net.vibmc.server.VibMC;

public class ExamplePlugin extends VibMCPlugin implements Listener {
    @Override
    public void onLoad() {
        getLogger().info("ExamplePlugin loading...");
    }

    @Override
    public void onEnable() {
        getLogger().info("ExamplePlugin enabled!");

        VibMC.getInstance().getPluginManager().registerEvents(this, this);

        getLogger().info("ExamplePlugin is ready!");
        getLogger().info("Players online: " + VibMC.getInstance().getPlayerManager().getOnlineCount());
    }

    @Override
    public void onDisable() {
        getLogger().info("ExamplePlugin disabled!");
    }

    @EventHandler(priority = 5)
    public void onPlayerJoin(PlayerJoinEvent event) {
        getLogger().info(event.getPlayer().getUsername() + " joined the server!");
        event.getPlayer().sendMessage("{\"text\":\"§aWelcome to the vib-MC server!\"}");
        event.getPlayer().sendMessage("{\"text\":\"§7Type §f/help §7for commands.\"}");
    }

    @EventHandler(priority = 5)
    public void onPlayerQuit(PlayerQuitEvent event) {
        getLogger().info(event.getPlayer().getUsername() + " left the server!");
    }

    @EventHandler(priority = 0)
    public void onChat(ChatEvent event) {
        getLogger().info("Chat: <" + event.getPlayer().getUsername() + "> " + event.getMessage());
    }

    @EventHandler(priority = 0)
    public void onBlockBreak(BlockBreakEvent event) {
        getLogger().info(event.getPlayer().getUsername() + " broke a block at " +
            event.getPosition().getX() + ", " + event.getPosition().getY() + ", " + event.getPosition().getZ());
    }

    @EventHandler(priority = 10)
    public void onTick(TickEvent.End event) {
        // Monitor TPS
        int tps = VibMC.getInstance().getMetricsManager().getCurrentTps();
        if (tps < 10) {
            getLogger().warn("Low TPS warning: " + tps);
        }
    }
}
