package re.imc.geysermodelengine.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import org.bukkit.entity.Entity;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Set;

public class AddEntityPacketListener extends PacketAdapter {
    public AddEntityPacketListener() {
        super(GeyserModelEngine.getInstance(), ListenerPriority.HIGHEST, Set.of(PacketType.Play.Server.SPAWN_ENTITY), ListenerOptions.SYNC);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        StructureModifier<Entity> modifier = packet.getEntityModifier(event);
        Entity entity = modifier.readSafely(0);
        if (entity == null) {
            return;
        }
        ModelEntity model = ModelEntity.MODEL_ENTITIES.get(entity.getEntityId());

        if (model != null && FloodgateApi.getInstance().isFloodgatePlayer(event.getPlayer().getUniqueId())) {
            model.getTask().sendEntityData(event.getPlayer(), GeyserModelEngine.getInstance().getSkinSendDelay());
        }
    }
}
