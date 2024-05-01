package re.imc.geysermodelengine.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.Set;

public class MountPacketListener extends PacketAdapter {
    public MountPacketListener() {
        super(GeyserModelEngine.getInstance(), ListenerPriority.HIGHEST, Set.of(PacketType.Play.Client.STEER_VEHICLE, PacketType.Play.Client.ENTITY_ACTION), ListenerOptions.SYNC);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!FloodgateApi.getInstance().isFloodgatePlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        if (event.getPacket().getType() == PacketType.Play.Client.STEER_VEHICLE) {
            Pair<ActiveModel, Mount> seat = GeyserModelEngine.getInstance().getDrivers().get(event.getPlayer());
            if (seat != null) {
                float pitch = event.getPlayer().getPitch();
                if (seat.getFirst().getModeledEntity().getBase().isFlying()) {
                    if (pitch < -30) {
                        event.getPacket().getBooleans().writeSafely(0, true);
                    }
                    if (pitch > 45) {
                        event.getPacket().getBooleans().writeSafely(1, true);
                    }
                } else {
                    if (event.getPlayer().getInventory().getHeldItemSlot() == 0) {
                        event.getPacket().getBooleans().writeSafely(0, true);
                        event.getPlayer().getInventory().setHeldItemSlot(3);
                        event.getPlayer().sendActionBar("jump");
                    }
                    if (pitch > 89 || event.getPlayer().getInventory().getHeldItemSlot() == 1) {
                        event.getPacket().getBooleans().writeSafely(1, true);
                        event.getPlayer().sendActionBar("shift");
                    }
                    if (event.getPlayer().getInventory().getHeldItemSlot() == 8) {
                        event.getPacket().getBooleans().writeSafely(0, true);
                        event.getPlayer().sendActionBar("hold jump");
                    }
                }
            }
        } else {
            Pair<ActiveModel, Mount> seat = GeyserModelEngine.getInstance().getDrivers().get(event.getPlayer());
            if (seat != null) {
                if (event.getPacket().getPlayerActions().read(0) == EnumWrappers.PlayerAction.START_SNEAKING) {
                    event.getPlayer().sendActionBar("leave");
                    seat.getSecond().clearPassengers();
                }
            }
        }
    }
}
