package net.vibmc.plugin.event;

import net.vibmc.player.Player;

public record PlayerJoinEvent(Player player) implements Event {
}
