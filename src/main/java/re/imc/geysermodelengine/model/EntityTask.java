package re.imc.geysermodelengine.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.render.DisplayRenderer;
import lombok.Getter;
import lombok.Setter;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.joml.Vector3f;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.packet.entity.PacketEntity;
import re.imc.geysermodelengine.util.BooleanPacker;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static re.imc.geysermodelengine.model.ModelEntity.ENTITIES;
import static re.imc.geysermodelengine.model.ModelEntity.MODEL_ENTITIES;

@Getter
@Setter
public class EntityTask {
    ModelEntity model;

    int tick = 0;
    int syncTick = 0;

    boolean removed = false;

    float lastScale = -1.0f;
    Color lastColor = null;
    Map<String, Integer> lastIntSet = new ConcurrentHashMap<>();
    Cache<String, Boolean> lastPlayedAnim = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MILLISECONDS).build();

    private ScheduledFuture scheduledFuture;

    public EntityTask(ModelEntity model) {
        this.model = model;
    }
    public void runAsync() {
        PacketEntity entity = model.getEntity();
        if (entity.isDead()) {
            return;
        }

        PacketEntity packetEntity = model.getEntity();
        // packetEntity.setHeadYaw((float) Math.toDegrees(model.getModeledEntity().getYHeadRot()));
        // packetEntity.setHeadPitch((float) Math.toDegrees(model.getModeledEntity().getXHeadRot()));
        model.teleportToModel();

        Set<Player> viewers = model.getViewers();
        ActiveModel activeModel = model.getActiveModel();
        ModeledEntity modeledEntity = model.getModeledEntity();
        if (activeModel.isDestroyed() || activeModel.isRemoved()) {
            removed = true;
            entity.remove();

            ENTITIES.remove(modeledEntity.getBase().getEntityId());
            MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }


        if (tick % 5 == 0) {
            if (tick % 40 == 0) {
                for (Player viewer : Set.copyOf(viewers)) {
                    if (!canSee(viewer, model.getEntity())) {
                        viewers.remove(viewer);
                    }
                }
            }
        }

        tick ++;
        if (tick > 400) {
            tick = 0;
            sendHitBoxToAll();
        }

        // Optional<Player> player = viewers.stream().findAny();
        // if (player.isEmpty()) return

        if (viewers.isEmpty()) {
            return;
        }
        // updateEntityProperties(viewers, false);

        // do not actually use this, atleast bundle these up ;(
        sendScale(viewers, false);
        sendColor(viewers, false);


    }

    public void checkViewers(Set<Player> viewers) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(onlinePlayer.getUniqueId())) {

                if (canSee(onlinePlayer, model.getEntity())) {

                    if (!viewers.contains(onlinePlayer)) {
                        sendSpawnPacket(onlinePlayer);
                        viewers.add(onlinePlayer);
                    }
                } else {
                    if (viewers.contains(onlinePlayer)) {
                        model.getEntity().sendEntityDestroyPacket(Collections.singletonList(onlinePlayer));
                        viewers.remove(onlinePlayer);
                    }
                }
            }
        }

    }

    private void sendSpawnPacket(Player onlinePlayer) {
        EntityTask task = model.getTask();
        boolean firstJoined = GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(onlinePlayer) != null;

        if (firstJoined) {
            task.sendEntityData(onlinePlayer, GeyserModelEngine.getInstance().getJoinSendDelay() / 50);
        } else {
            task.sendEntityData(onlinePlayer, 5);
        }
    }

    public void sendEntityData(Player player, int delay) {
        EntityUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());
        GeyserModelEngine.getInstance().getScheduler().schedule(() -> {
            model.getEntity().sendSpawnPacket(Collections.singletonList(player));
            GeyserModelEngine.getInstance().getScheduler().schedule(() -> {
                sendHitBox(player);
                sendScale(Collections.singleton(player), true);
                sendColor(Collections.singleton(player), true);
                updateEntityProperties(Collections.singleton(player), true);
            }, 500, TimeUnit.MILLISECONDS);
        }, delay * 50L, TimeUnit.MILLISECONDS);
    }

    public void sendScale(Collection<Player> players, boolean firstSend) {
        if (players.isEmpty()) {
            return;
        }
        Vector3f scale = model.getActiveModel().getScale();
        float average = (scale.x + scale.y + scale.z) / 3;

        if (!firstSend) {
            if (average == lastScale) return;
        }
        for (Player player : players) {
            EntityUtils.sendCustomScale(player, model.getEntity().getEntityId(), average);
        }
        lastScale = average;
    }

    public void sendColor(Collection<Player> players, boolean firstSend) {
        if (players.isEmpty()) return;

        Color color = new Color(model.getActiveModel().getDefaultTint().asARGB());
        if (model.getActiveModel().isMarkedHurt()) {
            color = new Color(model.getActiveModel().getDamageTint().asARGB());
        }
        if (firstSend) {
            if (color.equals(lastColor)) return;
        }
        for (Player player : players) {
            EntityUtils.sendCustomColor(player, model.getEntity().getEntityId(), color);
        }
        lastColor = color;
    }


    public void updateEntityProperties(Collection<Player> players, boolean firstSend, String... forceAnims) {
        int entity = model.getEntity().getEntityId();
        Set<String> forceAnimSet = Set.of(forceAnims);

        Map<String, Boolean> boneUpdates = new HashMap<>();
        Map<String, Boolean> animUpdates = new HashMap<>();
        Set<String> anims = new HashSet<>();
        // if (GeyserModelEngine.getInstance().getEnablePartVisibilityModels().contains(model.getActiveModel().getBlueprint().getName())) {

        model.getActiveModel().getBlueprint().getBones().forEach((s, bone) -> {
            processBone(bone, boneUpdates);
        });
        // }


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

        for (String anim : lastPlayed) {
            animUpdates.put(anim, true);
        }


        if (boneUpdates.isEmpty() && animUpdates.isEmpty()) return;

        Map<String, Integer> intUpdates = new HashMap<>();
        int i = 0;
        for (Integer integer : BooleanPacker.mapBooleansToInts(boneUpdates)) {
            intUpdates.put("modelengine:bone" + i, integer);
            i++;
        }
        i = 0;
        for (Integer integer : BooleanPacker.mapBooleansToInts(animUpdates)) {
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

        // System.out.println("AN: " + animUpdates.size() + ", BO:" + boneUpdates.size());
        if (GeyserModelEngine.getInstance().isDebug()) {
            GeyserModelEngine.getInstance().getLogger().info(animUpdates.toString());
        }


        List<String> list = new ArrayList<>(boneUpdates.keySet());
        Collections.sort(list);

        for (Player player : players) {
            EntityUtils.sendIntProperties(player, entity, intUpdates);
        }
    }

    private void processBone(BlueprintBone bone, Map<String, Boolean> map) {
        String name = unstripName(bone).toLowerCase();
        if (name.equals("hitbox") ||
                name.equals("shadow") ||
                name.equals("mount") ||
                name.startsWith("p_") ||
                name.startsWith("b_") ||
                name.startsWith("ob_")) {
            return;
        }
        for (BlueprintBone blueprintBone : bone.getChildren().values()) {
            processBone(blueprintBone, map);
        }
        ModelBone activeBone = model.getActiveModel().getBones().get(bone.getName());

        boolean visible = false;
        if (activeBone != null) {
            visible = activeBone.isVisible();
        }
        map.put(name, visible);
    }
    private String unstripName(BlueprintBone bone) {
        String name = bone.getName();
        if (bone.getBehaviors().get("head") != null) {
            if (!bone.getBehaviors().get("head").isEmpty()) return "hi_" + name;
            return "h_" + name;
        }

        return name;
    }

    public void sendHitBoxToAll() {
        for (Player viewer : model.getViewers()) {
            EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.01f, 0.01f);
        }

    }

    public void sendHitBox(Player viewer) {
        float w = 0;

        if (model.getActiveModel().isShadowVisible()) {
            if (model.getActiveModel().getModelRenderer() instanceof DisplayRenderer displayRenderer) {
            //    w = displayRenderer.getHitbox().getShadowRadius().get();
            }
        }
        EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.02f, w);

    }

    public boolean hasAnimation(String animation) {
        ActiveModel activeModel = model.getActiveModel();
        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);
        return !(animationProperty == null);
    }


    private boolean canSee(Player player, PacketEntity entity) {
        if (!player.isOnline()) {
            return false;
        }
        if (GeyserModelEngine.getInstance().getJoinedPlayer() != null && GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(player) != null) {
            return false;
        }

        Location playerLocation = player.getLocation().clone();
        Location entityLocation = entity.getLocation().clone();
        playerLocation.setY(0);
        entityLocation.setY(0);
        if (playerLocation.getWorld() != entityLocation.getWorld()) {
            return false;
        }
        if (playerLocation.distanceSquared(entityLocation) > player.getSendViewDistance() * player.getSendViewDistance() * 48) {
            return false;
        }
        return true;
        /*
        if (entity.getLocation().getChunk() == player.getChunk()) {
            return true;
        }
        if (entity.getLocation().getWorld() != player.getWorld()) {
            return false;
        }
        if (player.getLocation().distanceSquared(entity.getLocation()) > player.getSimulationDistance() * player.getSimulationDistance() * 256) {
            return false;
        }
        if (player.getLocation().distance(entity.getLocation()) > model.getActiveModel().getModeledEntity().getBase().getRenderRadius()) {
            return false;
        }
        return true;
         */
    }

    public void cancel() {
        // syncTask.cancel();
        scheduledFuture.cancel(true);
    }

    public void run(GeyserModelEngine instance) {

        sendHitBoxToAll();

        Runnable asyncTask = () -> {
            checkViewers(model.getViewers());
            runAsync();
        };
        scheduledFuture = GeyserModelEngine.getInstance().getScheduler().scheduleAtFixedRate(asyncTask, 0, 20, TimeUnit.MILLISECONDS);

        //asyncTask.runTaskTimerAsynchronously(instance, 0, 0);
    }
}
