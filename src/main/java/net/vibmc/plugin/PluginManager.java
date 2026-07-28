package net.vibmc.plugin;

import net.vibmc.permission.PermissionManager;
import net.vibmc.plugin.event.*;
import net.vibmc.server.VibMC;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginManager {
    private final List<VibMCPlugin> plugins;
    private final Map<Class<? extends Event>, List<RegisteredListener>> listeners;
    private final PermissionManager permissionManager;

    public PluginManager() {
        this.plugins = new CopyOnWriteArrayList<>();
        this.listeners = new ConcurrentHashMap<>();
        this.permissionManager = new PermissionManager();
    }

    public void loadPlugins(String directory) {
        File pluginDir = new File(directory);
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
            return;
        }

        File[] files = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        for (File file : files) {
            try {
                loadPlugin(file);
            } catch (Exception e) {
                VibMC.getInstance().getLogger().severe("Failed to load plugin from %s: %s", file.getName(), e);
            }
        }
    }

    private void loadPlugin(File file) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()},
            getClass().getClassLoader());

        InputStream descStream = loader.getResourceAsStream("plugin.yml");
        if (descStream == null) {
            loader.close();
            VibMC.getInstance().getLogger().warn("Plugin %s has no plugin.yml", file.getName());
            return;
        }

        PluginDescription desc = loadDescription(descStream);
        descStream.close();

        if (desc == null) {
            loader.close();
            VibMC.getInstance().getLogger().warn("Invalid plugin.yml in %s", file.getName());
            return;
        }

        Class<?> mainClass = loader.loadClass(desc.getMain());
        if (!VibMCPlugin.class.isAssignableFrom(mainClass)) {
            loader.close();
            VibMC.getInstance().getLogger().warn("Plugin %s main class does not extend VibMCPlugin", file.getName());
            return;
        }

        VibMCPlugin plugin = (VibMCPlugin) mainClass.getDeclaredConstructor().newInstance();
        plugin.setDescription(desc);
        plugin.setDataFolder(new File(file.getParentFile(), desc.getName()));
        plugin.setPluginFile(file);

        plugins.add(plugin);
        VibMC.getInstance().getLogger().info("Loaded plugin %s v%s", desc.getName(), desc.getVersion());
    }

    private PluginDescription loadDescription(InputStream stream) {
        try {
            Properties props = new Properties();
            props.load(stream);
            String name = props.getProperty("name");
            String version = props.getProperty("version");
            String main = props.getProperty("main");
            String description = props.getProperty("description", "");
            String authorStr = props.getProperty("authors", props.getProperty("author", ""));
            List<String> authors = Arrays.asList(authorStr.split(","));
            String dependsStr = props.getProperty("depends", "");
            List<String> depends = dependsStr.isEmpty() ? new ArrayList<>() : Arrays.asList(dependsStr.split(","));
            if (name != null && version != null && main != null) {
                return new PluginDescription(name, version, main, authors, depends, description);
            }
        } catch (Exception e) {
            VibMC.getInstance().getLogger().warn("Error reading plugin.yml: %s", e.getMessage());
        }
        return null;
    }

    public void onLoad() {
        for (VibMCPlugin plugin : plugins) {
            try {
                plugin.onLoad();
            } catch (Exception e) {
                VibMC.getInstance().getLogger().severe("Error loading plugin %s: %s", plugin.getName(), e);
            }
        }
    }

    public void onEnable() {
        for (VibMCPlugin plugin : plugins) {
            try {
                plugin.onEnable();
                plugin.setEnabled(true);
                VibMC.getInstance().getLogger().info("Enabled plugin %s v%s", plugin.getName(), plugin.getVersion());
            } catch (Exception e) {
                VibMC.getInstance().getLogger().severe("Error enabling plugin %s: %s", plugin.getName(), e);
            }
        }
    }

    public void onDisable() {
        for (int i = plugins.size() - 1; i >= 0; i--) {
            VibMCPlugin plugin = plugins.get(i);
            try {
                plugin.onDisable();
                plugin.setEnabled(false);
            } catch (Exception e) {
                VibMC.getInstance().getLogger().severe("Error disabling plugin %s: %s", plugin.getName(), e);
            }
        }
    }

    public void registerEvents(Listener listener, VibMCPlugin plugin) {
        for (var method : listener.getClass().getMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) continue;

            if (Event.class.isAssignableFrom(params[0])) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) params[0];
                listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                        .add(new RegisteredListener(listener, method, plugin, annotation.priority()));
                sortListeners(eventClass);
            }
        }
    }

    private void sortListeners(Class<? extends Event> eventClass) {
        List<RegisteredListener> list = listeners.get(eventClass);
        if (list == null) return;
        list.sort(Comparator.comparingInt(RegisteredListener::getPriority));
    }

    public void fireEvent(Event event) {
        List<RegisteredListener> list = listeners.get(event.getClass());
        if (list == null) return;
        for (RegisteredListener listener : list) {
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled() && listener.isIgnoringCancelled()) {
                continue;
            }
            try {
                listener.getMethod().invoke(listener.getListener(), event);
            } catch (Exception e) {
                VibMC.getInstance().getLogger().severe("Error firing event %s: %s",
                    event.getClass().getSimpleName(), e);
            }
        }
    }

    public void fireTickStart() {
        fireEvent(new TickEvent.Start());
    }

    public void fireTickEnd() {
        fireEvent(new TickEvent.End());
    }

    public PermissionManager getPermissionManager() { return permissionManager; }

    public List<VibMCPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public VibMCPlugin getPlugin(String name) {
        for (VibMCPlugin plugin : plugins) {
            if (plugin.getName().equalsIgnoreCase(name)) return plugin;
        }
        return null;
    }
}
