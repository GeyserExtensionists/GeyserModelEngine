package re.imc.geysermodelengine.listener;

import com.ticxo.modelengine.api.events.AddModelEvent;
import com.ticxo.modelengine.api.events.ModelDismountEvent;
import com.ticxo.modelengine.api.events.ModelMountEvent;
import com.ticxo.modelengine.api.model.ActiveModel;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.data.ModelEntityData;

import java.util.Map;

public class ModelListener implements Listener {

    private final GeyserModelEngine plugin;

    public ModelListener(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAddModel(AddModelEvent event) {
        if (event.isCancelled()) return;

        plugin.getModelManager().create(event.getTarget(), event.getModel());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelMount(ModelMountEvent event) {
        Map<ActiveModel, ModelEntityData> map = plugin.getModelManager().getEntitiesCache().get(event.getVehicle().getModeledEntity().getBase().getEntityId());
        if (!event.isDriver()) return;

        ModelEntityData model = map.get(event.getVehicle());

        if (model != null && event.getPassenger() instanceof Player player) {
            plugin.getModelManager().getDriversCache().put(player.getUniqueId(), Pair.of(event.getVehicle(), event.getSeat()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelDismount(ModelDismountEvent event) {
        if (event.getPassenger() instanceof Player player) {
            plugin.getModelManager().getDriversCache().remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        world.getEntities().forEach(entity -> plugin.getModelManager().processEntities(entity));
    }

    /*
     / Temp fix till a better solution is found -
     / the issue is when a player logs out and the mob is there,
     / the player logs back in sometimes it can display as a pig only
     / this issues mainly comes from the functions from EntityTaskManager
     / sendSpawnPacket() and canSee()
     /
     / TheLividaProject - conclusion:
     / I'm assuming when a player joins the server the packet for mob spawning is instant so the client resyncs itself
     / hence why the pig is shown instead of going invisible and not displaying the texture of the modeled mob
    */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> plugin.getModelManager().getPlayerJoinedCache().add(player.getUniqueId()), 10);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;
        plugin.getModelManager().getPlayerJoinedCache().remove(player.getUniqueId());
    }
}
