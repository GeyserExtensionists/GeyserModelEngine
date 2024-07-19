package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class EntityHurtAnimationPacket implements WrapperPacket {

    private final int id;

    public EntityHurtAnimationPacket(int id) {
        this.id = id;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.HURT_ANIMATION);
        packet.getIntegers().write(0, id);
        packet.getFloat().write(0, 5f);
        return packet;
    }
}
