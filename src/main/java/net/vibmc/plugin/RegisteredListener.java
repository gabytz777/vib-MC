package net.vibmc.plugin;

import java.lang.reflect.Method;

public class RegisteredListener {
    private final Listener listener;
    private final Method method;
    private final VibMCPlugin plugin;
    private final EventPriority priority;
    private final boolean ignoreCancelled;

    public RegisteredListener(Listener listener, Method method, VibMCPlugin plugin, EventPriority priority) {
        this.listener = listener;
        this.method = method;
        this.plugin = plugin;
        this.priority = priority;
        this.ignoreCancelled = method.getAnnotation(EventHandler.class).ignoreCancelled();
    }

    public Listener getListener() {
        return listener;
    }

    public Method getMethod() {
        return method;
    }

    public VibMCPlugin getPlugin() {
        return plugin;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public boolean isIgnoringCancelled() {
        return ignoreCancelled;
    }
}
