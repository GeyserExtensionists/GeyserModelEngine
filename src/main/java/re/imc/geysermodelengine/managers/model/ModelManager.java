package re.imc.geysermodelengine.managers.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Entity;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.data.ModelEntityData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModelManager {

    private final GeyserModelEngine plugin;

    private final HashSet<UUID> playerJoinedCache = new HashSet<>();

    private final ConcurrentHashMap<Integer, Map<ActiveModel, ModelEntityData>> entitiesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ModelEntityData> modelEntitiesCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Pair<ActiveModel, Mount>> driversCache = new ConcurrentHashMap<>();

    public ModelManager(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    public void create(ModeledEntity entity, ActiveModel model) {
        ModelEntityData modelEntity = new ModelEntityData(plugin, entity, model);
        int id = entity.getBase().getEntityId();

        Map<ActiveModel, ModelEntityData> map = entitiesCache.computeIfAbsent(id, k -> new HashMap<>());

        for (Map.Entry<ActiveModel, ModelEntityData> entry : map.entrySet()) {
            if (entry.getKey() !=  model && entry.getKey().getBlueprint().getName().equals(model.getBlueprint().getName())) {
                return;
            }
        }

        map.put(model, modelEntity);
    }

    public void processEntities(Entity entity) {
        if (entitiesCache.containsKey(entity.getEntityId())) return;

        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null) return;

        Optional<ActiveModel> model = modeledEntity.getModels().values().stream().findFirst();
        model.ifPresent(m -> create(modeledEntity, m));
    }

    public HashSet<UUID> getPlayerJoinedCache() {
        return playerJoinedCache;
    }

    public ConcurrentHashMap<Integer, Map<ActiveModel, ModelEntityData>> getEntitiesCache() {
        return entitiesCache;
    }

    public ConcurrentHashMap<Integer, ModelEntityData> getModelEntitiesCache() {
        return modelEntitiesCache;
    }

    public ConcurrentHashMap<UUID, Pair<ActiveModel, Mount>> getDriversCache() {
        return driversCache;
    }
}
