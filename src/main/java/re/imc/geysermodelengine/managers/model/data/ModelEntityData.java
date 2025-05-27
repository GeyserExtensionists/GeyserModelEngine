package re.imc.geysermodelengine.managers.model.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.google.common.collect.Sets;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.packet.entity.PacketEntity;
import re.imc.geysermodelengine.runnables.EntityTaskRunnable;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ModelEntityData {

    private final GeyserModelEngine plugin;

    private PacketEntity entity;

    private final Set<Player> viewers = Sets.newConcurrentHashSet();

    private final ModeledEntity modeledEntity;

    private final ActiveModel activeModel;

    private EntityTaskRunnable entityTask;

    public ModelEntityData(GeyserModelEngine plugin, ModeledEntity modeledEntity, ActiveModel model) {
        this.plugin = plugin;

        this.modeledEntity = modeledEntity;
        this.activeModel = model;
        this.entity = spawnEntity();

        runEntityTask();
    }

    public void teleportToModel() {
        Location location = modeledEntity.getBase().getLocation();
        entity.teleport(location);
    }

    public PacketEntity spawnEntity() {
        entity = new PacketEntity(EntityTypes.PIG, viewers, modeledEntity.getBase().getLocation());
        return entity;
    }

    public void runEntityTask() {
        entityTask = new EntityTaskRunnable(plugin, this);
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, entityTask, 0, 20, TimeUnit.MILLISECONDS);
    }

    public PacketEntity getEntity() {
        return entity;
    }

    public Set<Player> getViewers() {
        return viewers;
    }

    public ModeledEntity getModeledEntity() {
        return modeledEntity;
    }

    public ActiveModel getActiveModel() {
        return activeModel;
    }

    public EntityTaskRunnable getEntityTask() {
        return entityTask;
    }
}
