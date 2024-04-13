package re.imc.geysermodelengine.listener;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.AddModelEvent;
import com.ticxo.modelengine.api.events.AnimationEndEvent;
import com.ticxo.modelengine.api.events.AnimationPlayEvent;
import com.ticxo.modelengine.api.events.RemoveModelEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.EntityTask;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Map;
import java.util.Optional;

public class ModelListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAddModel(AddModelEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Bukkit.getScheduler().runTask(GeyserModelEngine.getInstance(), () -> {
            ModelEntity.create(event.getTarget(), event.getModel());
        });

    }
    @EventHandler
    public void onRemoveModel(RemoveModelEvent event) {
        event.getTarget().getBase();

    }

    @EventHandler
    public void onEntityLoad(EntitiesLoadEvent event) {
        Bukkit.getScheduler()
                .runTaskLater(GeyserModelEngine.getInstance(), () -> {
                    for (Entity entity : event.getEntities()) {
                        if (!ModelEntity.ENTITIES.containsKey(entity.getEntityId())) {
                            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
                            if (modeledEntity != null) {
                                Optional<ActiveModel> model = modeledEntity.getModels().values().stream().findFirst();
                                model.ifPresent(m -> ModelEntity.create(modeledEntity, m));
                            }
                        }
                    }

                }, 20);
    }

    @EventHandler
    public void onAnimationPlay(AnimationPlayEvent event) {
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getModel().getModeledEntity().getBase().getEntityId());
        if (map == null) {
            return;
        }
        ModelEntity model = map.get(event.getModel());

        if (model != null) {
            EntityTask task = model.getTask();
            int p = (event.getProperty().isForceOverride() ? 80 : (event.getProperty().isOverride() ? 70 : 60));
            task.playAnimation(event.getProperty().getName(), p);
        }
    }

    @EventHandler
    public void onModelHurt(EntityDamageEvent event) {
        ModelEntity model = ModelEntity.MODEL_ENTITIES.get(event.getEntity().getEntityId());
        if (model != null) {
            if (!event.getEntity().hasMetadata("show_damage")) {
                event.setCancelled(true);
            }
            event.getEntity().removeMetadata("show_damage", GeyserModelEngine.getInstance());

            if (!model.getEntity().isDead()) {
                event.setDamage(0);
                model.getEntity().setHealth(model.getEntity().getMaxHealth());
            }
        }
    }

    @EventHandler
    public void onModelEntityHurt(EntityDamageEvent event) {
        Map<ActiveModel, ModelEntity> model = ModelEntity.ENTITIES.get(event.getEntity().getEntityId());
        if (model != null) {
            for (Map.Entry<ActiveModel, ModelEntity> entry : model.entrySet()) {
                if (!entry.getValue().getEntity().isDead()) {
                    entry.getValue().getEntity().setMetadata("show_damage", new FixedMetadataValue(GeyserModelEngine.getInstance(), true));
                    entry.getValue().getEntity().damage(0);
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
    }

     */

    @EventHandler
    public void onModelHit(ProjectileHitEvent event) {
        if (event.getHitEntity() == null) {
            return;
        }
        ModelEntity model = ModelEntity.MODEL_ENTITIES.get(event.getHitEntity().getEntityId());
        if (model != null) {

            event.setCancelled(true);
            model.getEntity().setHealth(model.getEntity().getMaxHealth());

        }
    }


    @EventHandler
    public void onAnimationEnd(AnimationEndEvent event) {

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GeyserModelEngine.getInstance().getJoinedPlayer().put(event.getPlayer(), true);
    }
}
