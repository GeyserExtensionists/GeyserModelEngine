package re.imc.geysermodelengine.managers.model.entity;

import org.bukkit.entity.Player;
import re.imc.geysermodelengine.managers.model.taskshandler.TaskHandler;
import re.imc.geysermodelengine.packet.entity.PacketEntity;

import java.util.Set;

public interface EntityData {

    /**
     * Teleports the packet entity to the model
     */
    void teleportToModel();

    /**
     * Gets the packet Entity
     */
    PacketEntity getEntity();

    /**
     * Gets the entity view of players
     */
    Set<Player> getViewers();

    /**
     * Get the entity task handler
     */
    TaskHandler getEntityTask();
}
