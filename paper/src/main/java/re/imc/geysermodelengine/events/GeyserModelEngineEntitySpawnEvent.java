package re.imc.geysermodelengine.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.managers.model.entity.EntityData;

public class GeyserModelEngineEntitySpawnEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final EntityData entityData;

    private final boolean firstJoined;

    public GeyserModelEngineEntitySpawnEvent(EntityData entityData, boolean firstJoined) {
        super(true);
        this.entityData = entityData;
        this.firstJoined = firstJoined;
    }

    public EntityData getEntityData() {
        return entityData;
    }

    public boolean isFirstJoined() {
        return firstJoined;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
