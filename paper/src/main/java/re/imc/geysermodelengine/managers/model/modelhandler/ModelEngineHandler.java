package re.imc.geysermodelengine.managers.model.modelhandler;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.listener.ModelEngineListener;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.entity.ModelEngineEntityData;
import re.imc.geysermodelengine.managers.model.model.Model;
import re.imc.geysermodelengine.managers.model.model.ModelEngineModel;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModelEngineHandler implements ModelHandler {

    //TODO move driver hashmap here

    private final GeyserModelEngine plugin;

    public ModelEngineHandler(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createModel(Object... objects) {
        ModeledEntity megEntity = (ModeledEntity) objects[0];
        ActiveModel megActiveModel = (ActiveModel) objects[1];

        int entityID = megEntity.getBase().getEntityId();
        String name = megActiveModel.getBlueprint().getName();

        // Checked before creating the entity data: creating it starts its task, which would spawn an untracked entity.
        // A removed model with the same name doesn't count, so a model can be added again right after it was removed.
        Map<Model, EntityData> entityDataCache = plugin.getModelManager().getEntitiesCache().get(entityID);
        if (entityDataCache != null) {
            for (Model existing : entityDataCache.keySet()) {
                if (!existing.getName().equals(name)) continue;
                if (!(existing.getEntityData() instanceof ModelEngineEntityData existingData)) continue;

                ActiveModel existingModel = existingData.getActiveModel();
                if (!existingModel.isDestroyed() && !existingModel.isRemoved()) return;
            }
        }

        PropertyHandler propertyHandler = plugin.getEntityTaskManager().getPropertyHandler();
        EntityData entityData = new ModelEngineEntityData(plugin, megEntity, megActiveModel);

        Model model = new ModelEngineModel(megActiveModel, this, entityData, propertyHandler);

        plugin.getModelManager().getModelEntitiesCache().put(entityID, model);
        plugin.getModelManager().getEntitiesCache().compute(entityID, (id, models) -> {
            Map<Model, EntityData> map = models != null ? models : new ConcurrentHashMap<>();
            map.put(model, entityData);
            return map;
        });

        if (plugin.getConfigManager().getConfig().getBoolean("options.debug.spawn")) plugin.getLogger().info("Creating model for " + model.getName());
    }

    @Override
    public void processEntities(Entity entity) {
        if (plugin.getModelManager().getEntitiesCache().containsKey(entity.getEntityId())) return;

        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null) return;

        Optional<ActiveModel> model = modeledEntity.getModels().values().stream().findFirst();
        model.ifPresent(m -> createModel(modeledEntity, m));
    }

    // TODO ModelEngine canSee impl

    @Override
    public void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new ModelEngineListener(plugin), plugin);
    }
}
