package re.imc.geysermodelengine.runnables;

import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.model.Model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateTaskRunnable implements Runnable {

    private final GeyserModelEngine plugin;

    public UpdateTaskRunnable(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ConcurrentHashMap<Integer, Map<Model, EntityData>> entitiesCache = plugin.getModelManager().getEntitiesCache();
        if (entitiesCache.isEmpty()) return;

        try {
            for (Map<Model, EntityData> models : entitiesCache.values()) {
                models.values().forEach(entityData -> {
                    if (entityData.getViewers().isEmpty()) return;
                    plugin.getEntityTaskManager().getPropertyHandler().updateEntityProperties(entityData, entityData.getViewers(), false);
                });
            }
        } catch (Throwable err) {
            throw new RuntimeException(err);
        }
    }
}
