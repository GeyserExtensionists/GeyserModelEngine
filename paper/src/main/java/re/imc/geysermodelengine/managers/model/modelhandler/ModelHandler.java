package re.imc.geysermodelengine.managers.model.modelhandler;

import org.bukkit.entity.Entity;

public interface ModelHandler {

    /**
     * Creates the model from the required Model Engine
     * @param objects Processes the required objects
     */
    void createModel(Object... objects);

    /**
     * Processes entities into createModel()
     * @param entity Registers bukkit entities
     */
    void processEntities(Entity entity);

    /**
     * Loads the required listeners
     */
    void loadListeners();
}
