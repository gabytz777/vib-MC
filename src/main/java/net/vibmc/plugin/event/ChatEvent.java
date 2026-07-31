package net.vibmc.plugin.event;

import net.vibmc.player.Player;

public record ChatEvent(Player source, String message) implements Event {
}
