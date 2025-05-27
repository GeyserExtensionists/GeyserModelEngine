package re.imc.geysermodelengine.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.io.File;

public class ConfigManager {

    private final GeyserModelEngine plugin;

    private FileConfiguration config, lang;

    public ConfigManager(GeyserModelEngine plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        this.config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        this.lang = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "Lang/messages.yml"));
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLang() {
        return lang;
    }
}
