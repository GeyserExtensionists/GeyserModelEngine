package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.events.PacketContainer;

public interface WrapperPacket {

    default WrapperPacket decode() { return null; };
    default PacketContainer encode() { return null; };
}
