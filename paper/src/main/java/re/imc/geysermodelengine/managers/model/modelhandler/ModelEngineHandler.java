package re.imc.geysermodelengine.managers.model.modelhandler;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.listener.ModelEngineListener;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.entity.ModelEngineEntityData;
import re.imc.geysermodelengine.managers.model.model.Model;
import re.imc.geysermodelengine.managers.model.model.ModelEngineModel;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        PropertyHandler propertyHandler = plugin.getEntityTaskManager().getPropertyHandler();
        EntityData entityData = new ModelEngineEntityData(plugin, megEntity, megActiveModel);

        Model model = new ModelEngineModel(megActiveModel, this, entityData, propertyHandler);

        Map<Model, EntityData> entityDataCache = plugin.getModelManager().getEntitiesCache().computeIfAbsent(entityID, k -> new HashMap<>());

        for (Map.Entry<Model, EntityData> entry : entityDataCache.entrySet()) {
            if (entry.getKey() != model && entry.getKey().getName().equals(megActiveModel.getBlueprint().getName())) {
                return;
            }
        }

        plugin.getModelManager().getModelEntitiesCache().put(entityID, model);
        entityDataCache.put(model, entityData);

        if (plugin.getConfigManager().getConfig().getBoolean("options.debug")) plugin.getLogger().info("Creating model for " + model.getName());
    }

    @Override
    public void processEntities(Entity entity) {
        if (plugin.getModelManager().getEntitiesCache().containsKey(entity.getEntityId())) return;

        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
        if (modeledEntity == null) return;

        Optional<ActiveModel> model = modeledEntity.getModels().values().stream().findFirst();
        model.ifPresent(m -> createModel(modeledEntity, m));
    }

    @Override
    public void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new ModelEngineListener(plugin), plugin);
    }
}
