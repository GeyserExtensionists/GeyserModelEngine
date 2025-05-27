package re.imc.geysermodelengine.managers.bedrock;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

public class BedrockMountControlManager {

    private final ConcurrentHashMap<Player, Pair<ActiveModel, Mount>> driversCache = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Player, Pair<ActiveModel, Mount>> getDriversCache() {
        return driversCache;
    }
}
