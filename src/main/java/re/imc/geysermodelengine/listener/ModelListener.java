package re.imc.geysermodelengine.listener;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.*;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.render.ModelRenderer;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.EntityTask;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModelListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAddModel(AddModelEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!GeyserModelEngine.getInstance().isInitialized()) {
            return;
        }

        Bukkit.getScheduler().runTask(GeyserModelEngine.getInstance(), () -> {
            ModelEntity.create(event.getTarget(), event.getModel());
        });

    }


    @EventHandler
    public void onRemoveModel(RemoveModelEvent event) {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelMount(ModelMountEvent event) {
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getVehicle().getModeledEntity().getBase().getEntityId());
        if (map == null) {
            return;
        }
        if (!event.isDriver()) {
            return;
        }
        ModelEntity model = map.get(event.getVehicle());

        if (model != null && event.getPassenger() instanceof Player player) {
            GeyserModelEngine.getInstance().getDrivers().put(player, new Pair<>(event.getVehicle(), event.getSeat()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelDismount(ModelDismountEvent event) {
        if (event.getPassenger() instanceof Player player) {
            GeyserModelEngine.getInstance().getDrivers().remove(player);
        }
    }





    /*
    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelEntityHurt(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Map<ActiveModel, ModelEntity> model = ModelEntity.ENTITIES.get(event.getEntity().getEntityId());
        if (model != null) {
            for (Map.Entry<ActiveModel, ModelEntity> entry : model.entrySet()) {
                if (!entry.getValue().getEntity().isDead()) {
                    //entry.getValue().getEntity().sendHurtPacket(entry.getValue().getViewers());
                }
            }

        }
    }

     */
    /*

    @EventHandler
    public void onModelAttack(EntityDamageByEntityEvent event) {
        ModelEntity model = ModelEntity.ENTITIES.get(event.getDamager().getEntityId());
        if (model != null) {
            EntityTask task = model.getTask();

            task.playAnimation("attack", 55);
        }
    }|

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAnimationPlay(AnimationPlayEvent event) {
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getModel().getModeledEntity().getBase().getEntityId());
        if (map == null) {
            return;
        }

        ModelEntity model = map.get(event.getModel());
        model.getTask().updateEntityProperties(model.getViewers(), false, event.getProperty().getName());
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAnimationEnd(AnimationEndEvent event) {
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getModel().getModeledEntity().getBase().getEntityId());
        if (map == null) {
            return;
        }

        ModelEntity model = map.get(event.getModel());
        model.getTask().updateEntityProperties(model.getViewers(), false, event.getProperty().);
    }
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GeyserModelEngine.getInstance().getJoinedPlayer().put(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GeyserModelEngine.getInstance().getDrivers().remove(event.getPlayer());
    }
}
