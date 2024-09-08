package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import java.util.Optional;

public class EntityMetadataPacket implements WrapperPacket {
    private final int id;
    private final String name;

    public EntityMetadataPacket(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, id);
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setOptionalChatComponent(0, Optional.of(WrappedChatComponent.fromLegacyText(name)) ,true);
        packet.getWatchableCollectionModifier().writeSafely(0, watcher.getWatchableObjects());
        return packet;
    }
}
