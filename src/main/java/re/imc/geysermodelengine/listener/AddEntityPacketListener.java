package re.imc.geysermodelengine.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.interaction.DynamicHitbox;
import com.ticxo.modelengine.api.interaction.InteractionTracker;
import com.ticxo.modelengine.api.model.bone.type.SubHitbox;
import com.ticxo.modelengine.api.nms.entity.HitboxEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.EntityTask;
import re.imc.geysermodelengine.model.ModelEntity;

import java.util.Set;
import java.util.UUID;

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
        boolean isBedrock = FloodgateApi.getInstance().isFloodgatePlayer(event.getPlayer().getUniqueId());
        ModelEntity model = ModelEntity.MODEL_ENTITIES.get(entity.getEntityId());


        if (model != null) {
            if (isBedrock) {
                if (packet.getMeta("delayed").isPresent()) {
                    return;
                }

                EntityTask task = model.getTask();
                int delay = 1;
                boolean firstJoined = GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(event.getPlayer()) != null;
                if (firstJoined) {
                    delay = GeyserModelEngine.getInstance().getJoinSendDelay();
                }
                if (task == null || firstJoined) {
                    Bukkit.getScheduler().runTaskLater(GeyserModelEngine.getInstance(), () -> {
                        model.getTask().sendEntityData(event.getPlayer(), GeyserModelEngine.getInstance().getSkinSendDelay());
                    }, delay);
                } else {
                    task.sendEntityData(event.getPlayer(), GeyserModelEngine.getInstance().getSkinSendDelay());
                }

                event.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(GeyserModelEngine.getInstance(), () -> {
                    packet.setMeta("delayed", 1);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), packet);
                }, delay + 2);
            } else {
                event.setCancelled(true);
            }
        }
    }
}
