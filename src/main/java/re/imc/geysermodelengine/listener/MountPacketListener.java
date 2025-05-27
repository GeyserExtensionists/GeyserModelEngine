package re.imc.geysermodelengine.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

public class MountPacketListener implements PacketListener {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ENTITY_ACTION) return;
        if (!FloodgateApi.getInstance().isFloodgatePlayer(event.getUser().getUUID())) return;

        Player player = event.getPlayer();

        WrapperPlayClientEntityAction action = new WrapperPlayClientEntityAction(event);
        Pair<ActiveModel, Mount> seat = GeyserModelEngine.getInstance().getDrivers().get(player);

        if (seat == null) return;
        if (action.getAction() != WrapperPlayClientEntityAction.Action.START_SNEAKING) return;

        ModelEngineAPI.getMountPairManager().tryDismount(event.getPlayer());
    }
}
