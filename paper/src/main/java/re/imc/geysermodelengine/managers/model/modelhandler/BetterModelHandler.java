package re.imc.geysermodelengine.managers.model.modelhandler;

import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.listener.BetterModelListener;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;
import re.imc.geysermodelengine.managers.model.entity.BetterModelEntityData;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.model.BetterModelModel;
import re.imc.geysermodelengine.managers.model.model.Model;

import java.util.HashMap;
import java.util.Map;

public class BetterModelHandler implements ModelHandler {

    private final GeyserModelEngine plugin;

    public BetterModelHandler(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    //TODO fix dupe issue - dupe happens when server restart
    @Override
    public void createModel(Object... objects) {
        BaseEntity entitySource = (BaseEntity) objects[0];
        Tracker tracker = (Tracker) objects[1];
        EntityTracker entityTracker = (EntityTracker) objects[2];

        int entityID = entitySource.id();

        PropertyHandler propertyHandler = plugin.getEntityTaskManager().getPropertyHandler();
        EntityData entityData = new BetterModelEntityData(plugin, entitySource, tracker, entityTracker);

        Model model = new BetterModelModel(tracker, this, entityData, propertyHandler);

        Map<Model, EntityData> entityDataCache = plugin.getModelManager().getEntitiesCache().computeIfAbsent(entityID, k -> new HashMap<>());

        for (Map.Entry<Model, EntityData> entry : entityDataCache.entrySet()) {
            if (entry.getKey() != model && entry.getKey().getName().equals(tracker.name())) {
                return;
            }
        }

        plugin.getModelManager().getModelEntitiesCache().put(entityID, model);
        entityDataCache.put(model, entityData);
    }

    @Override
    public void processEntities(Entity entity) {
//        if (plugin.getModelManager().getEntitiesCache().containsKey(entity.getEntityId())) return;
//
//        @NotNull Optional<EntityTrackerRegistry> modeledEntity = BetterModel.registry(entity);
//
//        modeledEntity.ifPresent(m -> createModel(modeledEntity.get().entity(), m.));
    }

    @Override
    public void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new BetterModelListener(plugin), plugin);
    }
}
