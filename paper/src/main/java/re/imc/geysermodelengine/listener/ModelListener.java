package re.imc.geysermodelengine.listener;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.util.BedrockUtils;

public class ModelListener implements Listener {

    private final GeyserModelEngine plugin;

    public ModelListener(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    /*
     / xSquishyLiam:
     / May change this into a better system?
    */
    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        world.getEntities().forEach(entity -> plugin.getModelManager().getModelHandler().processEntities(entity));
    }

    /*
     / xSquishyLiam:
     / A runDelay makes sure the client doesn't see pigs on login due to the client resyncing themselves back to normal
    */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!BedrockUtils.isBedrockPlayer(player)) return;
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> plugin.getModelManager().getPlayerJoinedCache().add(player.getUniqueId()), 10);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!BedrockUtils.isBedrockPlayer(player)) return;
        plugin.getModelManager().getPlayerJoinedCache().remove(player.getUniqueId());
    }
}
