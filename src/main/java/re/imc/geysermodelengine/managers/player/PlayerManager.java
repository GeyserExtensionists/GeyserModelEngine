package re.imc.geysermodelengine.managers.player;

import java.util.HashSet;
import java.util.UUID;

public class PlayerManager {

    private final HashSet<UUID> playerJoinedCache = new HashSet<>();

    public HashSet<UUID> getPlayerJoinedCache() {
        return playerJoinedCache;
    }
}
