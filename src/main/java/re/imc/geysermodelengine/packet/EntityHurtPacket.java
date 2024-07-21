package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class EntityHurtPacket implements WrapperPacket {

    private final int id;

    public EntityHurtPacket(int id) {
        this.id = id;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.DAMAGE_EVENT);
        packet.getIntegers().writeSafely(0, id);
        packet.getIntegers().writeSafely(1,0);
        packet.getIntegers().writeSafely(2, 0);
        packet.getIntegers().writeSafely(3, 0);
        packet.getBooleans().writeSafely(0, false);
        return packet;
    }
}
