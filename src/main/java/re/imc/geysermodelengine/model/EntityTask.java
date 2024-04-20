package re.imc.geysermodelengine.model;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import lombok.Setter;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.Set;
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

    String lastAnimation = "";
    boolean looping = false;

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
        if (model.getEntity().isDead()) {
            model.spawnEntity();
        }

        model.getEntity().setVisualFire(false);
        model.teleportToModel();
    }
    public void runAsync() {
        Entity entity = model.getEntity();
        Set<Player> viewers = model.getViewers();
        ActiveModel activeModel = model.getActiveModel();
        ModeledEntity modeledEntity = model.getModeledEntity();
        if (modeledEntity.isDestroyed() || !modeledEntity.getBase().isAlive()) {
            if (!modeledEntity.getBase().isAlive()) {

                if (!modeledEntity.isDestroyed()) {
                    String animation = hasAnimation("death") ? "death" : "idle";
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            entity.remove();
                        }
                    }.runTaskLater(GeyserModelEngine.getInstance(), Math.min(Math.max(playAnimation(animation, 99, 50f) - 1, 0), 200));
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            entity.remove();
                        }
                    }.runTask(GeyserModelEngine.getInstance());
                }
            }
            ENTITIES.remove(modeledEntity.getBase().getEntityId());
            MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }
        if (model.getEntity().isDead()) {
            ENTITIES.remove(modeledEntity.getBase().getEntityId());
            MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }
        /*
        if (waitingTick > 0) {
            waitingTick--;
        }
         */
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



            if (!spawnAnimationPlayed) {
                spawnAnimationPlayed = true;
                if (hasAnimation("spawn")) {
                    playAnimation("spawn", 99);
                } else {
                    playAnimation("idle", 0);
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
    }

    public void sendEntityData(Player player, int delay) {
        PlayerUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName());
        Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
            // PlayerUtils.sendCustomSkin(player, model.getEntity(), model.getActiveModel().getBlueprint().getName());
            playBedrockAnimation("animation." + model.getActiveModel().getBlueprint().getName() + "." + lastAnimation, looping, 0f);
            sendHitBox(player);
            sendScale(player);
            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                sendHitBox(player);
            }, 8);
        }, delay);
    }

    public void sendScale(Player player) {
        // todo?
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
        return playAnimation(animation, p, 5f);
    }
    public int playAnimation(String animation, int p, float blendTime) {

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
        looping = animationProperty.getLoopMode() == BlueprintAnimation.LoopMode.LOOP;;

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

            String id = "animation." + activeModel.getBlueprint().getName() + "." + animationProperty.getName();
            lastAnimation = id;

            animationCooldown.set((int) (animationProperty.getLength() * 20));
            if (delaySend) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () ->  playBedrockAnimation("animation." + activeModel.getBlueprint().getName() + "." + animationProperty.getName(), looping, blendTime), 2);
            } else {
                playBedrockAnimation(id, looping, blendTime);
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
    private void playBedrockAnimation(String animationId, boolean loop, float blendTime) {

        // model.getViewers().forEach(viewer -> viewer.sendActionBar("CURRENT AN:" + animationId));

        Entity entity = model.getEntity();
        Set<Player> viewers = model.getViewers();

        Animation.AnimationBuilder animation = Animation.builder()
                .animation(animationId)
                .blendOutTime(blendTime)
                .controller("controller.animation.armor_stand.wiggle");

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
