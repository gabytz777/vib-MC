package net.vibmc.plugin;

import net.vibmc.server.Server;

public interface Plugin {
    String name();

    default void onEnable(Server server) {
    }

    default void onDisable() {
    }

    default void onEvent(Object event) {
    }
}
