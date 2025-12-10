package re.imc.geysermodelengine.managers.model.taskshandler;

import org.bukkit.entity.Player;
import re.imc.geysermodelengine.managers.model.entity.EntityData;

public interface TaskHandler {

    /**
     * Runs the entity scheduler
     */
    void runAsync();

    /**
     * Spawns the entity to the player
     * @param entityData The data of the entity
     * @param player Sends the entity to the player
     * @param delay Delays sending the entity to the player
     */
    void sendEntityData(EntityData entityData, Player player, int delay);

    /**
     * Cancels the entity scheduler
     */
    void cancel();
}
