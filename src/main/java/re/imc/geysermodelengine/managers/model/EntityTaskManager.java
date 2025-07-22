package re.imc.geysermodelengine.managers.model;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.render.DisplayRenderer;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.joml.Vector3fc;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.data.ModelEntityData;
import re.imc.geysermodelengine.packet.entity.PacketEntity;
import re.imc.geysermodelengine.runnables.EntityTaskRunnable;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.*;

public class EntityTaskManager {

    private final GeyserModelEngine plugin;

    private final Method scaleMethod;

    public EntityTaskManager(GeyserModelEngine plugin) {
        this.plugin = plugin;

        try {
            this.scaleMethod = ActiveModel.class.getMethod("getScale");
        } catch (NoSuchMethodException err) {
            throw new RuntimeException(err);
        }
    }

    public String unstripName(BlueprintBone bone) {
        String name = bone.getName();
        if (bone.getBehaviors().get("head") != null) {
            if (!bone.getBehaviors().get("head").isEmpty()) return "hi_" + name;
            return "h_" + name;
        }

        return name;
    }

    public void sendScale(ModelEntityData model, Collection<Player> players, float lastScale, boolean firstSend) {
        try {
            if (players.isEmpty()) return;

            Vector3fc scale = (Vector3fc) scaleMethod.invoke(model.getActiveModel());

            float average = (scale.x() + scale.y() + scale.z()) / 3;

            if (!firstSend) {
                if (average == lastScale) return;
            }

            for (Player player : players) {
                EntityUtils.sendCustomScale(player, model.getEntity().getEntityId(), average);
            }
        } catch (Throwable ignored) {}
    }

    public void sendColor(ModelEntityData model, Collection<Player> players, Color lastColor, boolean firstSend) {
        if (players.isEmpty()) return;

        Color color = new Color(model.getActiveModel().getDefaultTint().asARGB());
        if (model.getActiveModel().isMarkedHurt()) color = new Color(model.getActiveModel().getDamageTint().asARGB());

        if (firstSend) {
            if (color.equals(lastColor)) return;
        }

        for (Player player : players) {
            EntityUtils.sendCustomColor(player, model.getEntity().getEntityId(), color);
        }
    }

    public void checkViewers(ModelEntityData model, Set<Player> viewers) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!FloodgateApi.getInstance().isFloodgatePlayer(onlinePlayer.getUniqueId())) continue;

            if (canSee(onlinePlayer, model.getEntity())) {
                if (!viewers.contains(onlinePlayer)) {
                    sendSpawnPacket(model, onlinePlayer);
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

    private void sendSpawnPacket(ModelEntityData model, Player onlinePlayer) {
        EntityTaskRunnable task = model.getEntityTask();
        boolean firstJoined = !plugin.getModelManager().getPlayerJoinedCache().contains(onlinePlayer.getUniqueId());

        if (firstJoined) {
            task.sendEntityData(model, onlinePlayer, plugin.getConfigManager().getConfig().getInt("join-send-delay") / 50);
        } else {
            task.sendEntityData(model, onlinePlayer, 5);
        }
    }

    public boolean canSee(Player player, PacketEntity entity) {
        if (!player.isOnline()) return false;
        if (!plugin.getModelManager().getPlayerJoinedCache().contains(player.getUniqueId())) return false;

        Location playerLocation = player.getLocation().clone();
        Location entityLocation = entity.getLocation().clone();
        playerLocation.setY(0);
        entityLocation.setY(0);

        if (playerLocation.getWorld() != entityLocation.getWorld()) return false;
        if (playerLocation.distanceSquared(entityLocation) > player.getSendViewDistance() * player.getSendViewDistance() * 48) return false;

        return true;
    }

    public void sendHitBoxToAll(ModelEntityData model) {
        for (Player viewer : model.getViewers()) {
            EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.01f, 0.01f);
        }
    }

    public void sendHitBox(ModelEntityData model, Player viewer) {
        float w = 0;

        if (model.getActiveModel().isShadowVisible()) {
            if (model.getActiveModel().getModelRenderer() instanceof DisplayRenderer displayRenderer) {
                //    w = displayRenderer.getHitbox().getShadowRadius().get();
            }
        }

        EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.02f, w);
    }

    public boolean hasAnimation(ModelEntityData model, String animation) {
        ActiveModel activeModel = model.getActiveModel();
        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);
        return !(animationProperty == null);
    }

    public Method getScaleMethod() {
        return scaleMethod;
    }
}
