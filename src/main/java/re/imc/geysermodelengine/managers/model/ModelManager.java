package re.imc.geysermodelengine.managers.model;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.data.ModelEntityData;
import re.imc.geysermodelengine.runnables.EntityTaskRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelManager {

    private final GeyserModelEngine plugin;

    private final ConcurrentHashMap<Integer, Map<ActiveModel, ModelEntityData>> entitiesCache = new ConcurrentHashMap<>();
    private final Map<Integer, ModelEntityData> modelEntitiesCache = new ConcurrentHashMap<>();

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

    public ConcurrentHashMap<Integer, Map<ActiveModel, ModelEntityData>> getEntitiesCache() {
        return entitiesCache;
    }

    public Map<Integer, ModelEntityData> getModelEntitiesCache() {
        return modelEntitiesCache;
    }
}
