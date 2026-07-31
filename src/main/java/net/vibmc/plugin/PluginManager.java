package net.vibmc.plugin;

import net.vibmc.server.Server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PluginManager {
    private final List<Plugin> plugins = new ArrayList<>();

    public void loadPlugins(File pluginsDirectory, Server server) throws IOException {
        if (!pluginsDirectory.exists()) {
            Files.createDirectories(pluginsDirectory.toPath());
        }
        File[] pluginFiles = pluginsDirectory.listFiles((dir, name) -> name.endsWith(".properties"));
        if (pluginFiles == null) {
            return;
        }

        for (File pluginFile : pluginFiles) {
            Properties properties = new Properties();
            try (java.io.InputStream inputStream = java.nio.file.Files.newInputStream(pluginFile.toPath())) {
                properties.load(inputStream);
            }
            String className = properties.getProperty("class");
            if (className == null || className.isBlank()) {
                continue;
            }
            try {
                Class<?> pluginClass = Class.forName(className);
                Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
                plugin.onEnable(server);
                plugins.add(plugin);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new IOException("Failed to load plugin " + pluginFile.getName(), e);
            }
        }
    }

    public void dispatch(Object event) {
        for (Plugin plugin : plugins) {
            plugin.onEvent(event);
        }
    }

    public void shutdown() {
        for (Plugin plugin : plugins) {
            plugin.onDisable();
        }
        plugins.clear();
    }
}
