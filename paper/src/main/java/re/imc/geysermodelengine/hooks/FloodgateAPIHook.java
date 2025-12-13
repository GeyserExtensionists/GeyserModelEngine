package re.imc.geysermodelengine.hooks;

import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

public class FloodgateAPIHook {

    private static FloodgateApi floodgateAPI;

    public static void loadHook(GeyserModelEngine plugin) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null || !plugin.getConfigManager().getConfig().getBoolean("options.hooks.floodgate", true)) {
            plugin.getLogger().info("Floodgate hook disabled!");
            return;
        }

        floodgateAPI = FloodgateApi.getInstance();
        plugin.getLogger().info("Floodgate hook enabled!");
    }

    public static FloodgateApi getAPI() {
        return floodgateAPI;
    }
}
