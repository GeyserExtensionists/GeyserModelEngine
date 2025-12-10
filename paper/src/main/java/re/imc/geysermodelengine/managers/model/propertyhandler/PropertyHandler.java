package re.imc.geysermodelengine.managers.model.propertyhandler;

import org.bukkit.entity.Player;
import re.imc.geysermodelengine.managers.model.entity.EntityData;

import java.awt.*;
import java.util.Collection;

public interface PropertyHandler {

    /**
     * Sends scale of the entity to the player
     * @param entityData The data of the entity
     * @param players Collection of players from the entity view
     * @param lastScale Sends the last scale to the player
     * @param firstSend Checks if it's the first time to send scale to the player
     */
    void sendScale(EntityData entityData, Collection<Player> players, float lastScale, boolean firstSend);

    /**
     * Sends a colour tint to the player
     * @param entityData The data of the entity
     * @param players Collection of players from the entity view
     * @param lastColor Sends the last colour to the player
     * @param firstSend Checks if it's the first time to send colour to the player
     */
    void sendColor(EntityData entityData, Collection<Player> players, Color lastColor, boolean firstSend);

    /**
     * Sends a hitbox to the player
     * @param entityData The data of the entity
     * @param player Sends the player the entity hitbox
     */
    void sendHitBox(EntityData entityData, Player player);

    /**
     * Updates the entity to all viewable players
     * @param entityData The data of the entity
     * @param players Collection of players from the entity view
     * @param firstSend Checks if it's the first time to send the entity to the player
     * @param forceAnims Forces the entity to do an animation
     */
    void updateEntityProperties(EntityData entityData, Collection<Player> players, boolean firstSend, String... forceAnims);
}
