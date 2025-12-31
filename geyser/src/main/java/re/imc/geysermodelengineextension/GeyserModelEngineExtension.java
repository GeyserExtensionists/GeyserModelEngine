package re.imc.geysermodelengineextension;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import re.imc.geysermodelengineextension.managers.ConfigManager;
import re.imc.geysermodelengineextension.managers.resourcepack.ResourcePackManager;

public class GeyserModelEngineExtension implements Extension {

    private static GeyserModelEngineExtension extension;

    private ConfigManager configManager;

    private ResourcePackManager resourcePackManager;

    @Subscribe
    public void onLoad(GeyserPreInitializeEvent event) {
        extension = this;

        loadManagers();

        resourcePackManager.loadPack();
    }

    @Subscribe
    public void onDefineCommand(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this)
                .name("reload")
                .source(CommandSource.class)
                .playerOnly(false)
                .description("GeyserModelExtension Reload Command")
                .permission("geysermodelengineextension.commands.reload")
                .executor((source, command, args) -> {
                    resourcePackManager.loadPack();
                    source.sendMessage(configManager.getLang().getString("commands.geysermodelengineextension.reload.successfully-reloaded"));
                })
                .build());
    }

    @Subscribe
    public void onPackLoad(GeyserDefineResourcePacksEvent event) {
        if (!configManager.getConfig().getBoolean("options.resource-pack.auto-load")) return;

        ResourcePack resourcePack = ResourcePack.create(PackCodec.path(resourcePackManager.getGeneratedPackZipPath()));
        event.register(resourcePack);
    }

    private void loadManagers() {
        this.configManager = new ConfigManager(this);
        this.resourcePackManager = new ResourcePackManager(this);
    }

    public static GeyserModelEngineExtension getExtension() {
        return extension;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
}
