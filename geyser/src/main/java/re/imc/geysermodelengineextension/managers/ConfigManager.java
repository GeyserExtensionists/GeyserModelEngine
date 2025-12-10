package re.imc.geysermodelengineextension.managers;

import re.imc.geysermodelengineextension.util.FileConfiguration;

public class ConfigManager {

    private FileConfiguration config, lang;

    public ConfigManager() {
        load();
    }

    public void load() {
        this.config = new FileConfiguration("config.yml");
        this.lang = new FileConfiguration("Lang/messages.yml");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLang() {
        return lang;
    }
}
