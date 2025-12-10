package re.imc.geysermodelengine.listener;

import kr.toxicity.model.api.event.CreateEntityTrackerEvent;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.BetterModelEntityData;
import re.imc.geysermodelengine.managers.model.model.Model;

public class BetterModelListener implements Listener {

    private final GeyserModelEngine plugin;

    public BetterModelListener(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelSpawn(CreateEntityTrackerEvent event) {
        plugin.getModelManager().getModelHandler().createModel(event.sourceEntity(), event.getTracker(), event.tracker());
    }

    @EventHandler
    public void onModelDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        Model model = plugin.getModelManager().getModelEntitiesCache().get(entity.getEntityId());
        if (model == null) return;

        BetterModelEntityData entityData = (BetterModelEntityData) model.getEntityData();

        entityData.setHurt(true);
    }
}
