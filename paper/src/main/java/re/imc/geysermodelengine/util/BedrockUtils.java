package re.imc.geysermodelengine.util;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.hooks.FloodgateAPIHook;

public class BedrockUtils {

    private static final FloodgateApi floodgateAPIHook = FloodgateAPIHook.getAPI();

    public static boolean isBedrockPlayer(Player player) {
        if (floodgateAPIHook != null) return floodgateAPIHook.isFloodgatePlayer(player.getUniqueId());
        String clientBrand = player.getClientBrandName();
        if (clientBrand == null) return false;
        return clientBrand.contains("Geyser");
    }
}
