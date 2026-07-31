package net.vibmc.plugin.event;

import net.vibmc.player.Player;

public record PlayerQuitEvent(Player player, String reason) implements Event {
}
