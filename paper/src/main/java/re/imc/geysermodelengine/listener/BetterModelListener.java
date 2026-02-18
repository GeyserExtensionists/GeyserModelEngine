package re.imc.geysermodelengine.listener;

import com.ticxo.modelengine.api.entity.EntityDataTrackers;
import kr.toxicity.model.api.bukkit.BetterModelBukkit;
import kr.toxicity.model.api.event.CreateEntityTrackerEvent;

import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.BetterModelEntityData;
import re.imc.geysermodelengine.managers.model.model.Model;

public class BetterModelListener implements Listener {

    private final GeyserModelEngine plugin;

    public BetterModelListener(GeyserModelEngine plugin) {
        this.plugin = plugin;

        onModelSpawn();
    }

    public void onModelSpawn() {
        BetterModelBukkit.platform().eventBus().subscribe(plugin, CreateEntityTrackerEvent.class, event -> {
            plugin.getLogger().warning(event.tracker().sourceEntity().location().world().toString());
            plugin.getModelManager().getModelHandler().createModel(event.tracker());
        });
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
