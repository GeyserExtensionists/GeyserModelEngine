package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;

public class EntityTeleportPacket implements WrapperPacket {

    private final int id;
    private final Location loc;

    public EntityTeleportPacket(int entityID, Location location) {
        this.id = entityID;
        this.loc = location;
    }
    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, this.id);
        packet.getDoubles().write(0, loc.getX());
        packet.getDoubles().write(1, loc.getY());
        packet.getDoubles().write(2, loc.getZ());
        return packet;
    }
}
