package net.vibmc.plugin;

import net.vibmc.plugin.event.EventHandler;

import java.lang.reflect.Method;

public class RegisteredListener {
    private final Listener listener;
    private final Method method;
    private final VibMCPlugin plugin;
    private final int priority;

    public RegisteredListener(Listener listener, Method method, VibMCPlugin plugin, int priority) {
        this.listener = listener;
        this.method = method;
        this.plugin = plugin;
        this.priority = priority;
    }

    public Listener getListener() { return listener; }
    public Method getMethod() { return method; }
    public VibMCPlugin getPlugin() { return plugin; }
    public int getPriority() { return priority; }

    public boolean isIgnoringCancelled() {
        EventHandler handler = method.getAnnotation(EventHandler.class);
        return handler != null && handler.ignoreCancelled();
    }
}
