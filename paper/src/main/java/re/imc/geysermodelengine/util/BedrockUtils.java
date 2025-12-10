package re.imc.geysermodelengine.util;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class BedrockUtils {

    private static FloodgateApi FLOODGATE_API;

    public static boolean isBedrockPlayer(Player player) {
        if (FLOODGATE_API != null) return FLOODGATE_API.isFloodgatePlayer(player.getUniqueId());
        return player.getClientBrandName().contains("Geyser");
    }

    public static FloodgateApi getFloodgateApi() {
        return FLOODGATE_API;
    }
}
