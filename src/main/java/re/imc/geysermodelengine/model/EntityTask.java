package re.imc.geysermodelengine.model;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.Getter;
import lombok.Setter;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.joml.Vector3f;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.packet.entity.PacketEntity;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static re.imc.geysermodelengine.model.ModelEntity.ENTITIES;
import static re.imc.geysermodelengine.model.ModelEntity.MODEL_ENTITIES;

@Getter
@Setter
public class EntityTask {

    ModelEntity model;

    int tick = 0;
    int syncTick = 0;

    AtomicInteger animationCooldown = new AtomicInteger(0);
    AtomicInteger currentAnimationPriority = new AtomicInteger(0);

    boolean spawnAnimationPlayed = false;
    boolean removed = false;

    float lastScale = -1.0f;
    Color lastColor = null;
    Map<ModelBone, Boolean> lastModelBoneSet = new HashMap<>();


    String lastAnimation = "";
    String currentAnimProperty = "anim_spawn";
    String lastAnimProperty = "";

    boolean looping = true;

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
        if (activeModel.isDestroyed() || activeModel.isRemoved() || !modeledEntity.getBase().isAlive()) {
            if (!activeModel.isRemoved() && hasAnimation("death")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removed = true;
                        entity.remove();
                    }
                }.runTaskLater(GeyserModelEngine.getInstance(), Math.min(Math.max(playAnimation("death", 999, 5f, true) - 3, 0), 200));
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removed = true;
                        entity.remove();
                    }
                }.runTask(GeyserModelEngine.getInstance());
            }

            ENTITIES.remove(modeledEntity.getBase().getEntityId());
            MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }

        if (!spawnAnimationPlayed) {
            spawnAnimationPlayed = true;
        }

        if (tick % 5 == 0) {

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (FloodgateApi.getInstance().isFloodgatePlayer(onlinePlayer.getUniqueId())) {

                    if (canSee(onlinePlayer, model.getEntity())) {

                        if (!viewers.contains(onlinePlayer)) {
                            sendSpawnPacket(onlinePlayer);
                            viewers.add(onlinePlayer);
                        }
                    } else {
                        if (viewers.contains(onlinePlayer)) {
                            entity.sendEntityDestroyPacket(Collections.singletonList(onlinePlayer));
                            viewers.remove(onlinePlayer);
                        }
                    }
                }
            }

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

        if (base.isStrafing() && hasAnimation("strafe")) {
            playAnimation("strafe", 50);
        } else if (base.isFlying() && hasAnimation("fly")) {
            playAnimation("fly", 40);
        } else if (base.isJumping() && hasAnimation("jump")) {
            playAnimation("jump", 30);
        } else if (base.isWalking() && hasAnimation("walk")) {
            setAnimationProperty("modelengine:anim_walk");
            // playAnimation("walk", 20);
        } else if (hasAnimation("idle")) {
            // playAnimation("idle", 0);
            setAnimationProperty("modelengine:anim_idle");
        }

        if (animationCooldown.get() > 0) {
            animationCooldown.decrementAndGet();
        }

        Optional<Player> player = viewers.stream().findAny();
        if (player.isEmpty()) return;

        // i think properties need send to all players
        // because lastSet
        viewers.forEach(viewer -> updateEntityProperties(player.get(), false));

        // do not actually use this, atleast bundle these up ;(
        sendScale(player.get(), true);
        sendColor(player.get(), true);
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
                model.getTask().sendEntityData(onlinePlayer, GeyserModelEngine.getInstance().getSendDelay());
            }, delay);
        } else {
            task.sendEntityData(onlinePlayer, GeyserModelEngine.getInstance().getSendDelay());
        }
    }

    public void sendEntityData(Player player, int delay) {
        PlayerUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());

        Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
            // PlayerUtils.sendCustomSkin(player, model.getEntity(), model.getActiveModel().getBlueprint().getName());
            model.getEntity().sendSpawnPacket(Collections.singletonList(player));

            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                if (looping) {
                    playBedrockAnimation(lastAnimation, Set.of(player), looping, 0f);
                }
                sendHitBox(player);
                sendScale(player, true);
                updateEntityProperties(player, true);
            }, 8);
        }, delay);
    }

    public void sendScale(Player player, boolean ignore) {
        if (player == null) return;

        Vector3f scale = model.getActiveModel().getScale();
        float average = (scale.x + scale.y + scale.z) / 3;
        if (average == lastScale) return;

        PlayerUtils.sendCustomScale(player, model.getEntity(), average);

        if (ignore) return;
        lastScale = average;
    }

    public void sendColor(Player player, boolean ignore) {
        if (player == null) return;

        Color color = new Color(model.getActiveModel().getDefaultTint().asARGB());
        if (color.equals(lastColor)) return;

        PlayerUtils.sendCustomColor(player, model.getEntity(), color);

        if (ignore) return;
        lastColor = color;
    }

    public void setAnimationProperty(String currentAnimProperty) {
        this.lastAnimProperty = currentAnimProperty;
        this.currentAnimProperty = currentAnimProperty;
    }

    public void updateEntityProperties(Player player, boolean ignore) {
        Entity entity = model.getEntity();

        Map<String, Boolean> updates = new HashMap<>();
        model.getActiveModel().getBones().forEach((s, bone) -> {
            if (!lastModelBoneSet.containsKey(bone))
                lastModelBoneSet.put(bone, !bone.isVisible());

            Boolean lastBone = lastModelBoneSet.get(bone);
            if (lastBone == null)
                return;

            if (!lastBone.equals(bone.isVisible()) || ignore) {
                String name = unstripName(bone).toLowerCase();
                updates.put(model.getActiveModel().getBlueprint().getName() + ":" + name, bone.isVisible());
                lastModelBoneSet.replace(bone, bone.isVisible());
            }

        });
        if (ignore || !lastAnimProperty.equals(currentAnimProperty)) {
            updates.put(lastAnimProperty, false);
            updates.put(currentAnimProperty, true);
        }
        if (updates.isEmpty()) return;
        PlayerUtils.sendBoolProperties(player, entity, updates);
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
            PlayerUtils.sendCustomHitBox(viewer, model.getEntity(), 0.01f, 0.01f);
        }

    }

    public void sendHitBox(Player viewer) {
        PlayerUtils.sendCustomHitBox(viewer, model.getEntity(), 0.01f, 0.01f);

    }

    public boolean hasAnimation(String animation) {
        ActiveModel activeModel = model.getActiveModel();
        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);
        return !(animationProperty == null);
    }

    public int playAnimation(String animation, int p) {
        return playAnimation(animation, p, 0, false);
    }
    public int playAnimation(String animation, int p, float blendTime, boolean forceLoop) {

        ActiveModel activeModel = model.getActiveModel();

        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);


        if (animationProperty == null) {
            return 0;
        }


        boolean play = false;
        if (currentAnimationPriority.get() < p) {
            currentAnimationPriority.set(p);
            play = true;
        } else if (animationCooldown.get() == 0) {
            play = true;
        }
        looping = forceLoop || animationProperty.getLoopMode() == BlueprintAnimation.LoopMode.LOOP;;

        if (lastAnimation.equals(animation)) {
            if (looping) {
                play = false;
            }
        }



        if (play) {
            setAnimationProperty("modelengine:anim_stop");
            model.getViewers().forEach(viewer -> updateEntityProperties(viewer, false));
            currentAnimationPriority.set(p);

            String id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + "." + animationProperty.getName().toLowerCase();
            lastAnimation = id;

            animationCooldown.set((int) (animationProperty.getLength() * 20));
            playBedrockAnimation(id, model.getViewers(), looping, blendTime);
        }
        return animationCooldown.get();
    }

    public void playStopBedrockAnimation(String animationId) {

        Entity entity = model.getEntity();
        Set<Player> viewers = model.getViewers();

        // model.getViewers().forEach(viewer -> viewer.sendActionBar("CURRENT AN:" + "STOP"));

        Animation.AnimationBuilder animation = Animation.builder()
                .stopExpression("!query.any_animation_finished")
                .animation(animationId)
                .nextState(animationId)
                .controller("controller.animation.armor_stand.wiggle")
                .blendOutTime(0f);

        for (Player viewer : viewers) {
            PlayerUtils.playEntityAnimation(viewer, animation.build(), entity);
        }

    }



    public void playBedrockAnimation(String animationId, Set<Player> viewers, boolean loop, float blendTime) {

        // model.getViewers().forEach(viewer -> viewer.sendActionBar("CURRENT AN:" + animationId));

        Entity entity = model.getEntity();

        Animation.AnimationBuilder animation = Animation.builder()
                .animation(animationId)
                .blendOutTime(blendTime);

        if (loop) {
            animation.nextState(animationId);
        }
        for (Player viewer : viewers) {
            PlayerUtils.playEntityAnimation(viewer, animation.build(), entity);
        }

    }

    private boolean canSee(Player player, Entity entity) {
        if (!player.isOnline()) {
            return false;
        }
        if (player.isDead()) {
            return false;
        }
        if (GeyserModelEngine.getInstance().getJoinedPlayer() != null && GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(player) != null) {
            return false;
        }

        if (entity.getChunk() == player.getChunk()) {
            return true;
        }

        if (player.getLocation().distanceSquared(entity.getLocation()) > player.getSimulationDistance() * player.getSimulationDistance() * 256) {
            return false;
        }
        if (player.getLocation().distance(entity.getLocation()) > GeyserModelEngine.getInstance().getViewDistance()) {
            return false;
        }
        return true;

    }

    public void cancel() {
        // syncTask.cancel();
        asyncTask.cancel();
    }

    public void run(GeyserModelEngine instance, int i) {

        String id = "";
        ActiveModel activeModel = model.getActiveModel();
        if (hasAnimation("spawn")) {
            id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + ".spawn";
        } else {
            id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + ".idle";
        }

        lastAnimation = id;
        sendHitBoxToAll();

        asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                runAsync();
            }
        };
        asyncTask.runTaskTimerAsynchronously(instance, i + 2, 0);
    }
}
