package re.imc.geysermodelengine.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountController;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.Set;

public class MountPacketListener extends PacketAdapter {
    public MountPacketListener() {
        super(GeyserModelEngine.getInstance(), ListenerPriority.HIGHEST, Set.of(PacketType.Play.Client.ENTITY_ACTION), ListenerOptions.ASYNC);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!FloodgateApi.getInstance().isFloodgatePlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        Pair<ActiveModel, Mount> seat = GeyserModelEngine.getInstance().getDrivers().get(event.getPlayer());
        if (seat != null) {
            if (event.getPacket().getPlayerActions().read(0) == EnumWrappers.PlayerAction.START_SNEAKING) {
                ModelEngineAPI.getMountPairManager().tryDismount(event.getPlayer());
            }
        }
    }
}
