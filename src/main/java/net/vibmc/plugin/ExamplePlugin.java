package net.vibmc.plugin;

import net.vibmc.plugin.event.TickEvent;
import net.vibmc.server.Server;

public class ExamplePlugin implements Plugin {
    @Override
    public String name() {
        return "example";
    }

    @Override
    public void onEnable(Server server) {
        System.out.println("Example plugin enabled");
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof TickEvent) {
            // Keep the example plugin quiet on tick events so the console stays usable.
            return;
        }
        System.out.println("ExamplePlugin received event: " + event.getClass().getSimpleName());
    }
}
