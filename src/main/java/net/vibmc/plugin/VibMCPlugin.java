package net.vibmc.plugin;

import net.vibmc.server.VibMC;

import java.io.File;
import java.util.logging.Logger;

public abstract class VibMCPlugin {
    private PluginDescription description;
    private File dataFolder;
    private File pluginFile;
    private boolean enabled;

    public abstract void onLoad();
    public abstract void onEnable();
    public abstract void onDisable();

    public PluginDescription getDescription() { return description; }
    void setDescription(PluginDescription desc) { this.description = desc; }

    public File getDataFolder() { return dataFolder; }
    void setDataFolder(File folder) { this.dataFolder = folder; }

    public File getPluginFile() { return pluginFile; }
    void setPluginFile(File file) { this.pluginFile = file; }

    public boolean isEnabled() { return enabled; }
    void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getName() { return description != null ? description.getName() : "Unknown"; }
    public String getVersion() { return description != null ? description.getVersion() : "0.0"; }

    public Logger getLogger() {
        return Logger.getLogger("Plugin:" + getName());
    }

    public VibMC getServer() {
        return VibMC.getInstance();
    }
}
