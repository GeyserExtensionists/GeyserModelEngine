package re.imc.geysermodelengine;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import re.imc.geysermodelengine.listener.AddEntityPacketListener;
import re.imc.geysermodelengine.listener.InteractPacketListener;
import re.imc.geysermodelengine.listener.ModelListener;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class GeyserModelEngine extends JavaPlugin {

    @Getter
    private static GeyserModelEngine instance;

    @Getter
    private static boolean alwaysSendSkin;

    @Getter
    private int skinSendDelay;

    @Getter
    private int viewDistance;

    @Getter
    private EntityType modelEntityType;

    @Getter
    private Cache<Player, Boolean> joinedPlayer;
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        // alwaysSendSkin = getConfig().getBoolean("always-send-skin");
        skinSendDelay = getConfig().getInt("skin-send-delay");
        viewDistance = getConfig().getInt("skin-view-distance");
        modelEntityType = EntityType.valueOf(getConfig().getString("model-entity-type", "BAT"));
        int joinedDelay = getConfig().getInt("join-send-delay");
        if (joinedDelay > 0) {
            joinedPlayer = CacheBuilder.newBuilder()
                    .expireAfterWrite(joinedDelay * 50L, TimeUnit.MILLISECONDS).build();
        }
        instance = this;
        ProtocolLibrary.getProtocolManager().addPacketListener(new InteractPacketListener());
        ProtocolLibrary.getProtocolManager().addPacketListener(new AddEntityPacketListener());

        Bukkit.getPluginManager().registerEvents(new ModelListener(), this);
        Bukkit.getScheduler()
                .runTaskLater(GeyserModelEngine.getInstance(), () -> {
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (!ModelEntity.ENTITIES.containsKey(entity.getEntityId())) {
                                ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
                                if (modeledEntity != null) {
                                    Optional<ActiveModel> model = modeledEntity.getModels().values().stream().findFirst();
                                    model.ifPresent(m -> ModelEntity.create(modeledEntity, m));
                                }
                            }
                        }
                    }

                }, 100);
    }

    @Override
    public void onDisable() {
        for (Map<ActiveModel, ModelEntity> entities : ModelEntity.ENTITIES.values()) {
            entities.forEach((model, modelEntity) -> {
                modelEntity.getEntity().remove();
            });
        }
        // Plugin shutdown logic
    }

}
