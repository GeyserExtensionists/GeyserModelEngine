package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

import java.util.Collections;

public class EntityDestroyPacket implements WrapperPacket {

    private final int id;

    public EntityDestroyPacket(int id) {
        this.id = id;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, Collections.singletonList(this.id));
        return packet;
    }
}
