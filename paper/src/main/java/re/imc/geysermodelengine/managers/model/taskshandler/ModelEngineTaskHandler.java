package re.imc.geysermodelengine.managers.model.taskshandler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.entity.ModelEngineEntityData;
import re.imc.geysermodelengine.packet.entity.PacketEntity;

import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ModelEngineTaskHandler implements TaskHandler {

    private final GeyserModelEngine plugin;

    private final ModelEngineEntityData entityData;

    private int tick = 0;
    private int syncTick = 0;

    private float lastScale = -1.0f;
    private Color lastColor = null;

    private boolean removed = false;

    private final ConcurrentHashMap<String, Integer> lastIntSet = new ConcurrentHashMap<>();
    private final Cache<String, Boolean> lastPlayedAnim = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MILLISECONDS).build();

    private ScheduledFuture scheduledFuture;

    public ModelEngineTaskHandler(GeyserModelEngine plugin, ModelEngineEntityData entityData) {
        this.plugin = plugin;
        this.entityData = entityData;

        plugin.getEntityTaskManager().sendHitBoxToAll(entityData);
        scheduledFuture = plugin.getSchedulerPool().scheduleAtFixedRate(this::runAsync, 0, 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runAsync() {
        if (removed || entityData == null) return;

        PacketEntity entity = entityData.getEntity();
        if (entity == null || entity.isDead()) return;

        plugin.getEntityTaskManager().checkViewers(entityData, entityData.getViewers());

        entityData.teleportToModel();

        Set<Player> viewers = entityData.getViewers();
        ActiveModel activeModel = entityData.getActiveModel();
        ModeledEntity modeledEntity = entityData.getModeledEntity();

        if (activeModel.isDestroyed() || activeModel.isRemoved()) {
            removed = true;
            entity.remove();

            plugin.getModelManager().getEntitiesCache().remove(modeledEntity.getBase().getEntityId());
            plugin.getModelManager().getModelEntitiesCache().remove(modeledEntity.getBase().getEntityId());

            if (plugin.getConfigManager().getConfig().getBoolean("options.debug.death")) plugin.getLogger().info(activeModel.getBlueprint().getName() + " has died, removing runAsync!");

            cancel();
            return;
        }

        if (tick % 5 == 0) {
            if (tick % 40 == 0) {
                viewers.removeIf(viewer -> !plugin.getEntityTaskManager().canSee(viewer, entityData.getEntity()));
            }
        }

        tick ++;
        if (tick > 400) {
            tick = 0;
            plugin.getEntityTaskManager().sendHitBoxToAll(entityData);
        }

        if (viewers.isEmpty()) return;

        plugin.getEntityTaskManager().getPropertyHandler().sendScale(entityData, viewers, lastScale, false);
        plugin.getEntityTaskManager().getPropertyHandler().sendColor(entityData, viewers, lastColor, false);
    }

    @Override
    public void sendEntityData(EntityData entityData, Player player, int delay) {
        ModelEngineEntityData modelEngineEntityData = (ModelEngineEntityData) entityData;

        EntityUtils.setCustomEntity(player, modelEngineEntityData.getEntity().getEntityId(), plugin.getConfigManager().getConfig().getString("models.namespace") + ":" + modelEngineEntityData.getActiveModel().getBlueprint().getName().toLowerCase());
        if (plugin.getConfigManager().getConfig().getBoolean("options.debug.send-data")) plugin.getLogger().info("Setting custom entity data for " + modelEngineEntityData.getActiveModel().getBlueprint().getName());

        plugin.getSchedulerPool().schedule(() -> {
            entityData.getEntity().sendSpawnPacket(Collections.singletonList(player));

            plugin.getSchedulerPool().schedule(() -> {
                plugin.getEntityTaskManager().getPropertyHandler().sendHitBox(entityData, player);

                plugin.getEntityTaskManager().getPropertyHandler().sendScale(entityData, Collections.singleton(player), lastScale, true);
                plugin.getEntityTaskManager().getPropertyHandler().sendColor(entityData, Collections.singleton(player), lastColor, true);

                plugin.getEntityTaskManager().getPropertyHandler().updateEntityProperties(entityData, Collections.singleton(player), true);
            }, 500, TimeUnit.MILLISECONDS);
        }, delay * 50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancel() {
        scheduledFuture.cancel(true);
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public void setSyncTick(int syncTick) {
        this.syncTick = syncTick;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public void setLastScale(float lastScale) {
        this.lastScale = lastScale;
    }

    public int getTick() {
        return tick;
    }

    public int getSyncTick() {
        return syncTick;
    }

    public void setLastColor(Color lastColor) {
        this.lastColor = lastColor;
    }

    public float getLastScale() {
        return lastScale;
    }

    public Color getLastColor() {
        return lastColor;
    }

    public boolean isRemoved() {
        return removed;
    }

    public ConcurrentHashMap<String, Integer> getLastIntSet() {
        return lastIntSet;
    }

    public Cache<String, Boolean> getLastPlayedAnim() {
        return lastPlayedAnim;
    }

    public ScheduledFuture getScheduledFuture() {
        return scheduledFuture;
    }
}
