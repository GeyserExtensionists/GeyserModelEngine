package re.imc.geysermodelengine.managers.model.model;

import com.ticxo.modelengine.api.model.ActiveModel;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.modelhandler.ModelHandler;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;

public class ModelEngineModel implements Model {

    private final ActiveModel activeModel;
    private final ModelHandler modelHandler;
    private final EntityData entityData;
    private final PropertyHandler propertyHandler;

    public ModelEngineModel(ActiveModel activeModel, ModelHandler modelHandler, EntityData entityData, PropertyHandler propertyHandler) {
        this.activeModel = activeModel;
        this.modelHandler = modelHandler;
        this.entityData = entityData;
        this.propertyHandler = propertyHandler;
    }

    @Override
    public String getName() {
        return activeModel.getBlueprint().getName();
    }

    @Override
    public ModelHandler getModelHandler() {
        return modelHandler;
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public PropertyHandler getPropertyHandler() {
        return propertyHandler;
    }

    public ActiveModel getActiveModel() {
        return activeModel;
    }
}
