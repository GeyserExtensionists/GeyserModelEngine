package re.imc.geysermodelengine.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.managers.model.entity.EntityData;

public class GeyserModelEngineEntityDeathEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final EntityData entityData;

    public GeyserModelEngineEntityDeathEvent(EntityData entityData) {
        super(true);
        this.entityData = entityData;
    }

    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
