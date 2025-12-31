package re.imc.geysermodelengineextension.managers;

import com.google.gson.JsonObject;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;
import re.imc.geysermodelengineextension.util.FileConfiguration;
import re.imc.geysermodelengineextension.util.FileUtils;

import java.io.File;
import java.util.HashMap;

public class ConfigManager {

    private final GeyserModelEngineExtension extension;

    private FileConfiguration config, lang;

    private HashMap<String, JsonObject> resourcePackTemplatesCache = new HashMap<>();

    public ConfigManager(GeyserModelEngineExtension extension) {
        this.extension = extension;

        load();
    }

    public void load() {
        this.config = new FileConfiguration("config.yml");
        this.lang = new FileConfiguration("Lang/messages.yml");

        FileUtils.createFiles(extension, "ResourcePack/Templates/packmanifest.json");

        createResourcePackTemplates();
    }

    private void createResourcePackTemplates() {
        HashMap<String, JsonObject> tempResourcePackTemplatesCache = new HashMap<>();

        for (File file : FileUtils.getAllFiles(extension.dataFolder().resolve("ResourcePack/Templates").toFile(), ".json")) {
            tempResourcePackTemplatesCache.put(file.getName().replace(".json", ""), FileUtils.getJsonObject(file));
        }

        this.resourcePackTemplatesCache = tempResourcePackTemplatesCache;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLang() {
        return lang;
    }

    public HashMap<String, JsonObject> getResourcePackTemplatesCache() {
        return resourcePackTemplatesCache;
    }
}
