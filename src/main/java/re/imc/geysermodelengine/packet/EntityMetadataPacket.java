package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class EntityMetadataPacket implements WrapperPacket {
    private final int id;

    public EntityMetadataPacket(int id) {
        this.id = id;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, id);
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        packet.getWatchableCollectionModifier().writeSafely(0, watcher.getWatchableObjects());
        return packet;
    }
}
