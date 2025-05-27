package re.imc.geysermodelengine.managers.player;

import org.bukkit.entity.Player;

import java.util.HashSet;

public class PlayerManager {

    private final HashSet<Player> playerJoinedCache = new HashSet<>();

    public HashSet<Player> getPlayerJoinedCache() {
        return playerJoinedCache;
    }
}
