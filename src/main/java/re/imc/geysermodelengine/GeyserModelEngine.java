package re.imc.geysermodelengine;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.ticxo.modelengine.api.model.ActiveModel;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import re.imc.geysermodelengine.listener.ModelListener;
import re.imc.geysermodelengine.listener.MountPacketListener;
import re.imc.geysermodelengine.managers.ConfigManager;
import re.imc.geysermodelengine.managers.commands.CommandManager;
import re.imc.geysermodelengine.managers.model.EntityTaskManager;
import re.imc.geysermodelengine.managers.model.ModelManager;
import re.imc.geysermodelengine.managers.model.data.ModelEntityData;
import re.imc.geysermodelengine.runnables.BedrockMountControlRunnable;
import re.imc.geysermodelengine.runnables.UpdateTaskRunnable;

import java.util.*;
import java.util.concurrent.*;

public class GeyserModelEngine extends JavaPlugin {

    private ConfigManager configManager;

    private CommandManager commandManager;

    private ModelManager modelManager;
    private EntityTaskManager entityTaskManager;

    private ScheduledExecutorService schedulerPool;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
        loadHooks();
        loadManagers();
        loadRunnables();

        PacketEvents.getAPI().getEventManager().registerListener(new MountPacketListener(this), PacketListenerPriority.NORMAL);

        Bukkit.getPluginManager().registerEvents(new ModelListener(this), this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();

        for (Map<ActiveModel, ModelEntityData> entities : modelManager.getEntitiesCache().values()) {
            entities.forEach((model, modelEntity) -> {
                modelEntity.getEntity().remove();
            });
        }

        CommandAPI.onDisable();
    }

    private void loadHooks() {
        PacketEvents.getAPI().init();
        CommandAPI.onEnable();
    }

    private void loadManagers() {
        this.configManager = new ConfigManager(this);

        this.commandManager = new CommandManager(this);

        this.modelManager = new ModelManager(this);
        this.entityTaskManager = new EntityTaskManager(this);
    }

    private void loadRunnables() {
        this.schedulerPool = Executors.newScheduledThreadPool(configManager.getConfig().getInt("thread-pool-size", 4));

        Bukkit.getAsyncScheduler().runAtFixedRate(this, new UpdateTaskRunnable(this), 10, configManager.getConfig().getLong("entity-position-update-period", 35), TimeUnit.MILLISECONDS);
        Bukkit.getAsyncScheduler().runAtFixedRate(this, new BedrockMountControlRunnable(this), 1, 1, TimeUnit.MILLISECONDS);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public EntityTaskManager getEntityTaskManager() {
        return entityTaskManager;
    }

    public ScheduledExecutorService getSchedulerPool() {
        return schedulerPool;
    }
}
