package re.imc.geysermodelengine.util;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Objects;

public class BedrockUtils {

    public static FloodgateApi FLOODGATE_API;

    public static boolean isBedrockPlayer(Player player) {
        if (FLOODGATE_API != null) {
            return FLOODGATE_API.isFloodgatePlayer(player.getUniqueId());
        }
        return Objects.equals(player.getClientBrandName(), "Geyser");
    }
}
