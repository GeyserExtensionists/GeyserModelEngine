package re.imc.geysermodelengine.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.entity.BukkitPlayer;
import org.bukkit.entity.Entity;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Set;

public class InteractPacketListener extends PacketAdapter {
    public InteractPacketListener() {
        super(GeyserModelEngine.getInstance(), ListenerPriority.HIGHEST, Set.of(PacketType.Play.Client.USE_ENTITY), ListenerOptions.SYNC);
    }


    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        StructureModifier<Entity> modifier = packet.getEntityModifier(event);
        Entity entity = modifier.readSafely(0);
        if (entity == null) {
            return;
        }
        ModelEntity model = ModelEntity.MODEL_ENTITIES.get(entity.getEntityId());

        if (model != null && model.getModeledEntity().getBase() instanceof BukkitEntity bukkitEntity) {
            modifier.writeSafely(0, bukkitEntity.getOriginal());

            event.setPacket(packet);
        }

    }


}
