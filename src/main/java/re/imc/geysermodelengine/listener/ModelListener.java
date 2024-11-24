package re.imc.geysermodelengine.listener;

import com.ticxo.modelengine.api.events.AddModelEvent;
import com.ticxo.modelengine.api.events.ModelDismountEvent;
import com.ticxo.modelengine.api.events.ModelMountEvent;
import com.ticxo.modelengine.api.events.RemoveModelEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Map;

public class ModelListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAddModel(AddModelEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!GeyserModelEngine.getInstance().isInitialized()) {
            return;
        }
        ModelEntity.create(event.getTarget(), event.getModel());
    }


    @EventHandler
    public void onRemoveModel(RemoveModelEvent event) {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelMount(ModelMountEvent event) {
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getVehicle().getModeledEntity().getBase().getEntityId());
        if (map == null) {
        }
        if (!event.isDriver()) {
            return;
        }
        ModelEntity model = map.get(event.getVehicle());

        if (model != null && event.getPassenger() instanceof Player player) {
            GeyserModelEngine.getInstance().getDrivers().put(player, Pair.of(event.getVehicle(), event.getSeat()));
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

}
