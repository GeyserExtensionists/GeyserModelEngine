package re.imc.geysermodelengine.managers.model.model;

import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.modelhandler.ModelHandler;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;

public interface Model {

    /**
     * Gets the model's name
     */
    String getName();

    /**
     * Gets the model's entity data
     */
    EntityData getEntityData();

    /**
     * Gets the model's model handler
     */
    ModelHandler getModelHandler();

    /**
     * Gets the model's property handler
     */
    PropertyHandler getPropertyHandler();
}
