package re.imc.geysermodelengine.hooks;

import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

public class FloodgateAPIHook {

    private static FloodgateApi floodgateAPI;

    public static void loadHook(GeyserModelEngine plugin) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) {
            plugin.getLogger().info("floodgate hook not enabled!");
            return;
        }

        floodgateAPI = FloodgateApi.getInstance();

        plugin.getLogger().info("Hooking into floodgate!");

    }

    public static FloodgateApi getAPI() {
        return floodgateAPI;
    }
}
