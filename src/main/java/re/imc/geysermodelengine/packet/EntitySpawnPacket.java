package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class EntitySpawnPacket implements WrapperPacket {

    private final int id;
    private final UUID uuid;
    private final EntityType type;
    private final Location location;
    public EntitySpawnPacket(int entityID, UUID uuid, EntityType type, Location location) {
        this.id = entityID;
        this.uuid = uuid;
        this.type = type;
        this.location = location;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getIntegers()
                .write(0, this.id);
        packet.getUUIDs()
                .write(0, this.uuid);
        packet.getDoubles()
                .write(0, this.location.getX())
                .write(1, this.location.getY())
                .write(2, this.location.getZ());
        packet.getBytes()
                .write(0, (byte) (this.location.getPitch() * 256.0F / 360.0F))
                .write(1, (byte) (this.location.getYaw() * 256.0F / 360.0F))
                .writeSafely(2, (byte) (this.location.getYaw() * 256.0F / 360.0F));

        packet.getEntityTypeModifier()
                .writeSafely(0, type);

        return packet;
    }
}
