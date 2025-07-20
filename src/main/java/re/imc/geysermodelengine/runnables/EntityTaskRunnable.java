package re.imc.geysermodelengine.runnables;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.data.ModelEntityData;
import re.imc.geysermodelengine.packet.entity.PacketEntity;
import re.imc.geysermodelengine.util.BooleanPacker;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EntityTaskRunnable {

    private final GeyserModelEngine plugin;

    private final ModelEntityData model;

    private int tick = 0;
    private int syncTick = 0;

    private float lastScale = -1.0f;
    private Color lastColor = null;

    private boolean removed = false;

    private final ConcurrentHashMap<String, Integer> lastIntSet = new ConcurrentHashMap<>();
    private final Cache<String, Boolean> lastPlayedAnim = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MILLISECONDS).build();

    private final BooleanPacker booleanPacker = new BooleanPacker();

    private ScheduledFuture scheduledFuture;

    public EntityTaskRunnable(GeyserModelEngine plugin, ModelEntityData model) {
        this.plugin = plugin;

        this.model = model;
    }

    public void run() {
        plugin.getEntityTaskManager().sendHitBoxToAll(model);

        Runnable asyncTask = () -> {
            try {
                runAsync();
            } catch (Throwable ignored) {}
        };

        scheduledFuture = plugin.getSchedulerPool().scheduleAtFixedRate(asyncTask, 0, 20, TimeUnit.MILLISECONDS);
    }

    public void runAsync() {
        plugin.getEntityTaskManager().checkViewers(model, model.getViewers());

        PacketEntity entity = model.getEntity();
        if (entity.isDead()) return;

        model.teleportToModel();

        Set<Player> viewers = model.getViewers();
        ActiveModel activeModel = model.getActiveModel();
        ModeledEntity modeledEntity = model.getModeledEntity();

        if (activeModel.isDestroyed() || activeModel.isRemoved()) {
            removed = true;
            entity.remove();

            plugin.getModelManager().getEntitiesCache().remove(modeledEntity.getBase().getEntityId());
            plugin.getModelManager().getModelEntitiesCache().remove(entity.getEntityId());
            cancel();
            return;
        }

        if (tick % 5 == 0) {
            if (tick % 40 == 0) {
                for (Player viewer : Set.copyOf(viewers)) {
                    if (!plugin.getEntityTaskManager().canSee(viewer, model.getEntity())) {
                        viewers.remove(viewer);
                    }
                }
            }
        }

        tick ++;
        if (tick > 400) {
            tick = 0;
            plugin.getEntityTaskManager().sendHitBoxToAll(model);
        }

        if (viewers.isEmpty()) return;

        plugin.getEntityTaskManager().sendScale(model, viewers, lastScale, false);
        plugin.getEntityTaskManager().sendColor(model, viewers, lastColor, false);
    }

    public void cancel() {
        scheduledFuture.cancel(true);
    }

    public void sendEntityData(ModelEntityData model, Player player, int delay) {
        //TODO with ModelEngine, you can define the namespace inside the config, make an option to change it here as well? if i'm right about this
        EntityUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());

        plugin.getSchedulerPool().schedule(() -> {
            model.getEntity().sendSpawnPacket(Collections.singletonList(player));

            plugin.getSchedulerPool().schedule(() -> {
                plugin.getEntityTaskManager().sendHitBox(model, player);
                plugin.getEntityTaskManager().sendScale(model, Collections.singleton(player), lastScale, true);
                plugin.getEntityTaskManager().sendColor(model, Collections.singleton(player), lastColor, true);

                updateEntityProperties(model, Collections.singleton(player), true);
            }, delay * 50L, TimeUnit.MILLISECONDS);
        }, 500, TimeUnit.MILLISECONDS);
    }

    public void updateEntityProperties(ModelEntityData model, Collection<Player> players, boolean firstSend, String... forceAnims) {
        int entity = model.getEntity().getEntityId();
        Set<String> forceAnimSet = Set.of(forceAnims);

        Map<String, Boolean> boneUpdates = new HashMap<>();
        Map<String, Boolean> animUpdates = new HashMap<>();
        Set<String> anims = new HashSet<>();

        model.getActiveModel().getBlueprint().getBones().forEach((s, bone) -> processBone(model, bone, boneUpdates));

        AnimationHandler handler = model.getActiveModel().getAnimationHandler();
        Set<String> priority = model.getActiveModel().getBlueprint().getAnimationDescendingPriority();
        for (String animId : priority) {
            if (handler.isPlayingAnimation(animId)) {
                BlueprintAnimation anim = model.getActiveModel().getBlueprint().getAnimations().get(animId);

                anims.add(animId);
                if (anim.isOverride() && anim.getLoopMode() == BlueprintAnimation.LoopMode.ONCE) {
                    break;
                }
            }
        }

        for (String id : priority) {
            if (anims.contains(id)) {
                animUpdates.put(id, true);
            } else {
                animUpdates.put(id, false);
            }
        }

        Set<String> lastPlayed = new HashSet<>(lastPlayedAnim.asMap().keySet());

        for (Map.Entry<String, Boolean> anim : animUpdates.entrySet()) {
            if (anim.getValue()) {
                lastPlayedAnim.put(anim.getKey(), true);
            }
        }

        for (String anim : lastPlayed) animUpdates.put(anim, true);

        if (boneUpdates.isEmpty() && animUpdates.isEmpty()) return;

        Map<String, Integer> intUpdates = new HashMap<>();
        int i = 0;
        for (Integer integer : booleanPacker.mapBooleansToInts(boneUpdates)) {
            intUpdates.put("modelengine:bone" + i, integer);
            i++;
        }

        i = 0;
        for (Integer integer : booleanPacker.mapBooleansToInts(animUpdates)) {
            intUpdates.put("modelengine:anim" + i, integer);
            i++;
        }

        if (!firstSend) {
            if (intUpdates.equals(lastIntSet)) {
                return;
            } else {
                lastIntSet.clear();
                lastIntSet.putAll(intUpdates);
            }
        }

        if (plugin.getConfigManager().getConfig().getBoolean("debug")) plugin.getLogger().info(animUpdates.toString());

        List<String> list = new ArrayList<>(boneUpdates.keySet());
        Collections.sort(list);

        for (Player player : players) {
            EntityUtils.sendIntProperties(player, entity, intUpdates);
        }
    }

    private void processBone(ModelEntityData model, BlueprintBone bone, Map<String, Boolean> map) {
        String name = plugin.getEntityTaskManager().unstripName(bone).toLowerCase();
        if (name.equals("hitbox") ||
                name.equals("shadow") ||
                name.equals("mount") ||
                name.startsWith("p_") ||
                name.startsWith("b_") ||
                name.startsWith("ob_")) {
            return;
        }

        for (BlueprintBone blueprintBone : bone.getChildren().values()) processBone(model, blueprintBone, map);

        ModelBone activeBone = model.getActiveModel().getBones().get(bone.getName());

        boolean visible = false;
        if (activeBone != null) visible = activeBone.isVisible();

        map.put(name, visible);
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
