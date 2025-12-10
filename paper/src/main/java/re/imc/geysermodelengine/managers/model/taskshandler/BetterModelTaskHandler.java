package re.imc.geysermodelengine.managers.model.taskshandler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.tracker.Tracker;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.BetterModelEntityData;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.packet.entity.PacketEntity;

import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BetterModelTaskHandler implements TaskHandler {

    private final GeyserModelEngine plugin;

    private final BetterModelEntityData entityData;

    private int tick = 0;
    private int syncTick = 0;

    private float lastScale = -1.0f;
    private Color lastColor = null;

    private boolean removed = false;

    private final ConcurrentHashMap<String, Integer> lastIntSet = new ConcurrentHashMap<>();
    private final Cache<String, Boolean> lastPlayedAnim = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MILLISECONDS).build();

    private ScheduledFuture scheduledFuture;

    public BetterModelTaskHandler(GeyserModelEngine plugin, BetterModelEntityData entityData) {
        this.plugin = plugin;
        this.entityData = entityData;

        plugin.getEntityTaskManager().sendHitBoxToAll(entityData);
        scheduledFuture = plugin.getSchedulerPool().scheduleAtFixedRate(this::runAsync, 0, 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runAsync() {
        plugin.getEntityTaskManager().checkViewers(entityData, entityData.getViewers());

        PacketEntity entity = entityData.getEntity();
        if (entity.isDead()) return;

        Set<Player> viewers = entityData.getViewers();
        BaseEntity entitySource = entityData.getEntitySource();
        Tracker tracker = entityData.getTracker();

        entityData.teleportToModel();

        if (entitySource.dead() || tracker.forRemoval()) {
            removed = true;
            entity.remove();

            plugin.getModelManager().getEntitiesCache().remove(entitySource.id());
            plugin.getModelManager().getModelEntitiesCache().remove(entitySource.id());

            cancel();
            return;
        }

        if (tick % 5 == 0) {
            if (tick % 40 == 0) {
                for (Player viewer : Set.copyOf(viewers)) {
                    if (!plugin.getEntityTaskManager().canSee(viewer, entityData.getEntity())) {
                        viewers.remove(viewer);
                    }
                }
            }
        }

        tick++;
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
        BetterModelEntityData betterModelEntityData = (BetterModelEntityData) entityData;

        EntityUtils.setCustomEntity(player, betterModelEntityData.getEntity().getEntityId(), plugin.getConfigManager().getConfig().getString("models.namespace") + ":" + betterModelEntityData.getTracker().name().toLowerCase());

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
