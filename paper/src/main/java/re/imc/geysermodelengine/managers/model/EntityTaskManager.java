package re.imc.geysermodelengine.managers.model;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.propertyhandler.BetterModelPropertyHandler;
import re.imc.geysermodelengine.managers.model.propertyhandler.ModelEnginePropertyHandler;
import re.imc.geysermodelengine.managers.model.propertyhandler.PropertyHandler;
import re.imc.geysermodelengine.managers.model.entity.ModelEngineEntityData;
import re.imc.geysermodelengine.managers.model.taskshandler.TaskHandler;
import re.imc.geysermodelengine.packet.entity.PacketEntity;
import re.imc.geysermodelengine.util.BedrockUtils;

import java.util.*;

public class EntityTaskManager {

    private final GeyserModelEngine plugin;

    private PropertyHandler propertyHandler;

    public EntityTaskManager(GeyserModelEngine plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("ModelEngine") != null) {
            this.propertyHandler = new ModelEnginePropertyHandler(plugin);
            plugin.getLogger().info("Using ModelEngine property handler!");
        } else if (Bukkit.getPluginManager().getPlugin("BetterModel") != null) {
            this.propertyHandler = new BetterModelPropertyHandler(plugin);
            plugin.getLogger().info("Using BetterModel property handler!");
        } else {
            plugin.getLogger().severe("No supported model engine found!");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public void checkViewers(EntityData model, Set<Player> viewers) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!BedrockUtils.isBedrockPlayer(onlinePlayer)) continue;

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

    private void sendSpawnPacket(EntityData model, Player onlinePlayer) {
        TaskHandler task = model.getEntityTask();
        boolean firstJoined = !plugin.getModelManager().getPlayerJoinedCache().contains(onlinePlayer.getUniqueId());

        if (firstJoined) {
            task.sendEntityData(model, onlinePlayer, plugin.getConfigManager().getConfig().getInt("models.join-send-delay") / 50);
            if (plugin.getConfigManager().getConfig().getBoolean("options.debug.spawn")) plugin.getLogger().info("Sending spawn Packet on first join entity ID: " + model.getEntity().getEntityId());
        } else {
            task.sendEntityData(model, onlinePlayer, 5);
            if (plugin.getConfigManager().getConfig().getBoolean("options.debug.spawn")) plugin.getLogger().info("Sending spawn Packet on spawn entity ID: " + model.getEntity().getEntityId());
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

    public void sendHitBoxToAll(EntityData model) {
        for (Player viewer : model.getViewers()) {
            EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.01f, 0.01f);
        }
    }

    //TODO move this
    public boolean hasAnimation(ModelEngineEntityData model, String animation) {
        ActiveModel activeModel = model.getActiveModel();
        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);
        return !(animationProperty == null);
    }

    public PropertyHandler getPropertyHandler() {
        return propertyHandler;
    }
}
