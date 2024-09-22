package re.imc.geysermodelengine.model;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.animation.handler.IStateMachineHandler;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.events.AnimationEndEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.Getter;
import lombok.Setter;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.joml.Vector3f;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.packet.entity.PacketEntity;
import re.imc.geysermodelengine.util.BooleanPacker;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static re.imc.geysermodelengine.model.ModelEntity.ENTITIES;
import static re.imc.geysermodelengine.model.ModelEntity.MODEL_ENTITIES;

@Getter
@Setter
public class EntityTask {

    private static final String STOP_ANIMATION_PROPERTY = "anim_stop";
    private static final Map<String, Boolean> ALL_PROPERTIES = Map.of("modelengine:anim_walk", false, "modelengine:anim_idle", false, "modelengine:anim_stop", false);
    ModelEntity model;

    int tick = 0;
    int syncTick = 0;

    boolean removed = false;

    float lastScale = -1.0f;
    Color lastColor = null;
    Map<String, Integer> lastIntSet = new HashMap<>();

    // Map<String, Boolean> lastAnimPropertySet = new HashMap<>();
    private BukkitRunnable syncTask;

    private BukkitRunnable asyncTask;



    public EntityTask(ModelEntity model) {
        this.model = model;
    }
    public void runAsync() {
        PacketEntity entity = model.getEntity();
        if (entity.isDead()) {
            return;
        }

        model.teleportToModel();
        Set<Player> viewers = model.getViewers();
        ActiveModel activeModel = model.getActiveModel();
        ModeledEntity modeledEntity = model.getModeledEntity();
        if (activeModel.isDestroyed() || activeModel.isRemoved()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    removed = true;
                    entity.remove();
                }
            }.runTaskLater(GeyserModelEngine.getInstance(), 1);


            ENTITIES.remove(modeledEntity.getBase().getEntityId());
            MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }


        if (tick % 5 == 0) {

            checkViewers(viewers);

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

        BaseEntity<?> base = modeledEntity.getBase();
        // Optional<Player> player = viewers.stream().findAny();
        // if (player.isEmpty()) return

        updateEntityProperties(viewers, false);

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
        int delay = 1;
        boolean firstJoined = GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(onlinePlayer) != null;
        if (firstJoined) {
            delay = GeyserModelEngine.getInstance().getJoinSendDelay();
        }
        if (task == null || firstJoined) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                model.getTask().sendEntityData(onlinePlayer, 1);
            }, delay);
        } else {
            task.sendEntityData(onlinePlayer, 1);
        }
    }

    public void sendEntityData(Player player, int delay) {
        EntityUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());
        Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
            model.getEntity().sendSpawnPacket(Collections.singletonList(player));
            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                sendHitBox(player);
                sendScale(Collections.singleton(player), true);
                sendColor(Collections.singleton(player), true);
                updateEntityProperties(Collections.singleton(player), true);
            }, 1);
        }, delay);
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
        if (firstSend) {
            if (color.equals(lastColor)) return;
        }
        for (Player player : players) {
            EntityUtils.sendCustomColor(player, model.getEntity().getEntityId(), color);
        }
        lastColor = color;
    }


    public void updateEntityProperties(Collection<Player> players, boolean ignore) {
        int entity = model.getEntity().getEntityId();

        Map<String, Boolean> boneUpdates = new HashMap<>();
        Map<String, Boolean> animUpdates = new HashMap<>();

        // if (GeyserModelEngine.getInstance().getEnablePartVisibilityModels().contains(model.getActiveModel().getBlueprint().getName())) {
        model.getActiveModel().getBones().forEach((s, bone) -> {
            String name = unstripName(bone).toLowerCase();
            if (name.equals("hitbox") ||
                    name.equals("shadow") ||
                    name.equals("mount") ||
                    name.startsWith("p_") ||
                    name.startsWith("b_") ||
                    name.startsWith("ob_")) {
                return;
            }
            boneUpdates.put(name, bone.isVisible());
        });
        // }

        model.getActiveModel().getBlueprint().getAnimations().forEach((s, anim) -> {
            animUpdates.put(s, model.getActiveModel().getAnimationHandler().isPlayingAnimation(s));
        });

        /*f
        if (!lastAnimProperty.equals(currentAnimProperty)) {

            player.sendMessage("CHANGED");
            if (animationCooldown.get() == 0) {
                player.sendMessage(lastAnimProperty + " -> " + currentAnimProperty);

                updates.put("modelengine:" + lastAnimProperty, false);
                updates.put("modelengine:" + currentAnimProperty, true);
            } else {
                updates.put("modelengine:" + lastAnimProperty, false);
                updates.put("modelengine:" + STOP_ANIMATION_PROPERTY, true);
            }
        }

         */
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
        if (!ignore) {
            if (intUpdates.equals(lastIntSet)) {
                return;
            } else {
                lastIntSet = intUpdates;

            }
        }
        /*
        System.out.println("AN: " + animUpdates.size() + ", BO:" + boneUpdates.size());
        System.out.println(animUpdates);
        List<String> list = new ArrayList<>(boneUpdates.keySet());
        Collections.sort(list);
        System.out.println(list);
        System.out.println(boneUpdates);
        System.out.println(intUpdates);

         */
        for (Player player : players) {
            EntityUtils.sendIntProperties(player, entity, intUpdates);
        }
    }

    private String unstripName(ModelBone bone) {
        String name = bone.getBoneId();
        if (bone.getBlueprintBone().getBehaviors().get("head") != null) {
            if (!bone.getBlueprintBone().getBehaviors().get("head").isEmpty()) return "hi_" + name;
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
        EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.01f, 0.01f);

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
        if (player.isDead()) {
            return false;
        }
        if (GeyserModelEngine.getInstance().getJoinedPlayer() != null && GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(player) != null) {
            return false;
        }
        CullType type = model.getActiveModel().getModeledEntity().getBase().getData().getTracking().get(player);
        return type != null;
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
        asyncTask.cancel();
    }

    public void run(GeyserModelEngine instance) {
        sendHitBoxToAll();

        asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                runAsync();
            }
        };
        asyncTask.runTaskTimerAsynchronously(instance, 0, 0);
    }
}
