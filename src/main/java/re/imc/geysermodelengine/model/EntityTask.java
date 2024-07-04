package re.imc.geysermodelengine.model;

import com.google.common.base.Joiner;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.Getter;
import lombok.Setter;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.GeyserUtils;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.listener.ModelListener;

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

    boolean firstAnimation = true;
    boolean spawnAnimationPlayed = false;
    boolean removed = false;
    boolean registered = false;

    float lastScale = -1.0f;
    Map<ModelBone, Boolean> lastSet = new HashMap<>();

    String lastAnimation = "";
    boolean looping = true;

    private BukkitRunnable syncTask;

    private BukkitRunnable asyncTask;


    public EntityTask(ModelEntity model) {
        this.model = model;
    }

    public void runSync() {

        syncTick ++;
        if (syncTick > 400) {
            syncTick = 0;
        }

        if (syncTick % 5 == 0) {

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!FloodgateApi.getInstance().isFloodgatePlayer(onlinePlayer.getUniqueId())) {
                    onlinePlayer.hideEntity(GeyserModelEngine.getInstance(), model.getEntity());
                }
            }
        }

        if (!removed && model.getEntity().isDead() && model.getModeledEntity().getBase().isAlive() && !model.getActiveModel().isRemoved()) {
            // model.spawnEntity();
        }

        model.getEntity().setVisualFire(false);
        model.teleportToModel();
    }
    public void runAsync() {
        Entity entity = model.getEntity();
        if (entity.isDead()) {
            return;
        }
        Set<Player> viewers = model.getViewers();
        ActiveModel activeModel = model.getActiveModel();
        ModeledEntity modeledEntity = model.getModeledEntity();
        if (activeModel.isRemoved() || !modeledEntity.getBase().isAlive()) {
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
        /*
        if (model.getEntity().isDead()) {
            ENTITIES.remove(modeledEntity.getBase().getEntityId());
            MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }

         */
        /*
        if (waitingTick > 0) {
            waitingTick--;
        }
         */

        if (!spawnAnimationPlayed) {
            spawnAnimationPlayed = true;
        }

        if (tick > 1 && tick % 5 == 0) {

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (FloodgateApi.getInstance().isFloodgatePlayer(onlinePlayer.getUniqueId())) {

                    if (canSee(onlinePlayer, model.getEntity())) {

                        if (!viewers.contains(onlinePlayer)) {
                            viewers.add(onlinePlayer);
                            /*
                            if (GeyserModelEngine.getInstance().getSkinSendDelay() > 0) {
                                sendEntityData(onlinePlayer, GeyserModelEngine.getInstance().getSkinSendDelay());
                            } else {
                                PlayerUtils.sendCustomSkin(onlinePlayer, model.getEntity(), activeModel.getBlueprint().getName());

                                Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                                    sendHitBox(onlinePlayer);
                                }, 2);
                            }

                             */
                        }
                    } else {
                        viewers.remove(onlinePlayer);
                    }
                }
            }

            if (tick % 40 == 0) {

                for (Player viewer : Set.copyOf(viewers)) {

                    if (!canSee(viewer, model.getEntity())) {
                        viewers.remove(viewer);
                    }

                    /*

                    if (GeyserModelEngine.isAlwaysSendSkin()) {
                        PlayerUtils.sendCustomSkin(viewer, model.getEntity(), activeModel.getBlueprint().getName());

                    }

                     */
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
            playAnimation("walk", 20);
        } else if (hasAnimation("idle")) {
            playAnimation("idle", 0);
        }

        if (animationCooldown.get() > 0) {
            animationCooldown.decrementAndGet();
        }

        Optional<Player> player = model.getViewers().stream().findAny();
        if (player.isEmpty()) return;

        sendScale(player.get());
        updateVisibility(player.get());
    }

    public void sendEntityData(Player player, int delay) {
        // System.out.println("TYPE: " + "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());
        PlayerUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());

        Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
            // PlayerUtils.sendCustomSkin(player, model.getEntity(), model.getActiveModel().getBlueprint().getName());
            if (looping) {
                playBedrockAnimation(lastAnimation, Set.of(player), looping, 0f);
            }
            sendHitBox(player);
            sendScale(player);
            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                sendHitBox(player);
            }, 8);
        }, delay);
    }

    public void sendScale(Player player) {
        if (player == null) return;

        Vector3f scale = model.getActiveModel().getScale();
        float average = (scale.x + scale.y + scale.z) / 3;
        if (average == lastScale) return;

        PlayerUtils.sendCustomScale(player, model.getEntity(), average);
        lastScale = average;
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
        boolean delaySend = false;
        if (firstAnimation) {
            delaySend = true;
            firstAnimation = false;
        }
        boolean lastLoopState = looping;
        looping = forceLoop || animationProperty.getLoopMode() == BlueprintAnimation.LoopMode.LOOP;;

        if (lastAnimation.equals(animation)) {
            if (looping) {
                // play = waitingTick == 1;
                play = false;
            }
        }



        if (play) {
            currentAnimationPriority.set(p);

            if (lastLoopState && !lastAnimation.equals(animation)) {
                // clearLoopAnimation();
                // delaySend = true;
            }

            String id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + "." + animationProperty.getName().toLowerCase();
            lastAnimation = id;

            animationCooldown.set((int) (animationProperty.getLength() * 20));
            if (delaySend) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () ->  playBedrockAnimation(id, model.getViewers(), looping, blendTime), 0);
            } else {
                playBedrockAnimation(id, model.getViewers(), looping, blendTime);
            }
        }
        return animationCooldown.get();
    }

    /*
    private void clearLoopAnimation() {
        playStopBedrockAnimation(lastAnimation);

    }


    private void playStopBedrockAnimation(String animationId) {

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


    */
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

    public void updateVisibility(Player player) {
        Entity entity = model.getEntity();

        // pretty sure it gets set on the entity, so no need to send it for every single viewer (small pyramid btw)
        model.getActiveModel().getBones().forEach((s,bone) -> {
            if (!lastSet.containsKey(bone)) lastSet.put(bone, !bone.isVisible());

            if (!lastSet.get(bone).equals(bone.isVisible())) {
                PlayerUtils.sendBoolProperty(player,entity,
                        model.getActiveModel().getBlueprint().getName() + ":" + bone.getBoneId().toLowerCase(), bone.isVisible());
                lastSet.replace(bone, bone.isVisible());

                Bukkit.getLogger().info(model.getActiveModel().getBlueprint().getName() + ":" + bone.getBoneId().toLowerCase());
            }

        });
    }


    private boolean canSee(Player player, Entity entity) {
        if (!player.isOnline()) {
            return false;
        }
        if (player.isDead()) {
            return false;
        }
        if (player.getWorld() != entity.getWorld()) {
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
        syncTask.cancel();
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
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                runSync();
            }
        };
        syncTask.runTaskTimer(instance, i, 0);

        asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                runAsync();
            }
        };
        asyncTask.runTaskTimerAsynchronously(instance, i + 2, 0);
    }
}
