package re.imc.geysermodelengine.managers.model;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.model.Model;
import re.imc.geysermodelengine.managers.model.modelhandler.BetterModelHandler;
import re.imc.geysermodelengine.managers.model.modelhandler.ModelEngineHandler;
import re.imc.geysermodelengine.managers.model.modelhandler.ModelHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModelManager {

    private final GeyserModelEngine plugin;

    private ModelHandler modelHandler;

    private final HashSet<UUID> playerJoinedCache = new HashSet<>();

    private final ConcurrentHashMap<Integer, Model> modelEntitiesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Map<Model, EntityData>> entitiesCache = new ConcurrentHashMap<>();

    // MEG ONLY
    private final ConcurrentHashMap<UUID, Pair<ActiveModel, Mount>> driversCache = new ConcurrentHashMap<>();

    public ModelManager(GeyserModelEngine plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("ModelEngine") != null) {
            this.modelHandler = new ModelEngineHandler(plugin);
            plugin.getLogger().info("Using ModelEngine handler!");
        } else if (Bukkit.getPluginManager().getPlugin("BetterModel") != null) {
            this.modelHandler = new BetterModelHandler(plugin);
            plugin.getLogger().info("Using BetterModel handler!");
        } else {
            plugin.getLogger().severe("No supported model engine found!");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        modelHandler.loadListeners();
    }

    /**
     * Forgets one model of an entity. The entity can carry other models (for example one added later with
     * addModel), and those stay tracked: the entity itself is only forgotten once its last model is gone.
     */
    public void removeModel(int entityId, EntityData entityData) {
        entitiesCache.computeIfPresent(entityId, (id, models) -> {
            models.values().removeIf(data -> data == entityData);
            return models.isEmpty() ? null : models;
        });

        modelEntitiesCache.computeIfPresent(entityId, (id, model) -> {
            if (model.getEntityData() != entityData) return model;

            Map<Model, EntityData> remaining = entitiesCache.get(entityId);
            return remaining == null ? null : remaining.keySet().stream().findFirst().orElse(null);
        });
    }

    public void removeEntities() {
        for (Map<Model, EntityData> entities : entitiesCache.values()) {
            entities.forEach((model, modelEntity) -> modelEntity.getEntity().remove());
        }
    }

    public ModelHandler getModelHandler() {
        return modelHandler;
    }

    public HashSet<UUID> getPlayerJoinedCache() {
        return playerJoinedCache;
    }

    public ConcurrentHashMap<Integer, Map<Model, EntityData>> getEntitiesCache() {
        return entitiesCache;
    }

    public ConcurrentHashMap<Integer, Model> getModelEntitiesCache() {
        return modelEntitiesCache;
    }

    public ConcurrentHashMap<UUID, Pair<ActiveModel, Mount>> getDriversCache() {
        return driversCache;
    }
}
