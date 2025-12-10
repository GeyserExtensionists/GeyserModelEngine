package re.imc.geysermodelengine.managers.model.model;

import kr.toxicity.model.api.tracker.Tracker;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.modelhandler.ModelHandler;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;

public class BetterModelModel implements Model {

    private final Tracker tracker;
    private final ModelHandler modelHandler;
    private final EntityData entityData;
    private final PropertyHandler propertyHandler;

    public BetterModelModel(Tracker tracker, ModelHandler modelHandler, EntityData entityData, PropertyHandler propertyHandler) {
        this.tracker = tracker;
        this.modelHandler = modelHandler;
        this.entityData = entityData;
        this.propertyHandler = propertyHandler;
    }

    @Override
    public String getName() {
        return tracker.name();
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
}
