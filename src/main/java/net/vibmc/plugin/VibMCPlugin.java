package net.vibmc.plugin;

import net.vibmc.command.CommandManager;
import net.vibmc.server.VibMC;
import net.vibmc.server.util.Logger;

import java.io.File;

public abstract class VibMCPlugin {
    private PluginDescription description;
    private File dataFolder;
    private File pluginFile;
    private boolean enabled;

    public void onLoad() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public PluginDescription getDescription() {
        return description;
    }

    public void setDescription(PluginDescription description) {
        this.description = description;
    }

    public String getName() {
        return description != null ? description.getName() : "Unknown";
    }

    public String getVersion() {
        return description != null ? description.getVersion() : "unknown";
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public File getPluginFile() {
        return pluginFile;
    }

    public void setPluginFile(File pluginFile) {
        this.pluginFile = pluginFile;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Logger getLogger() {
        return VibMC.getInstance().getLogger();
    }

    public PluginManager getPluginManager() {
        return VibMC.getInstance().getPluginManager();
    }

    public CommandManager getCommandManager() {
        return VibMC.getInstance().getCommandManager();
    }
}
