package re.imc.geysermodelengine.packet.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.EntityPositionData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityMetadataProvider;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class PacketEntity {

    private int id;
    private UUID uuid;
    private EntityType type;
    private Set<Player> viewers;
    private Location location;
    private float headYaw;
    private float headPitch;

    private boolean removed = false;

    public PacketEntity(EntityType type, Set<Player> viewers, Location location) {
        this.id = ThreadLocalRandom.current().nextInt(300000000, 400000000);
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.viewers = viewers;
        this.location = location;
    }

    public @NotNull Location getLocation() {
        return location;
    }

    public boolean teleport(@NotNull Location location) {
        boolean sent = this.location.getWorld() != location.getWorld() || this.location.distanceSquared(location) > 0.000001 || this.location.getYaw() != location.getYaw() || this.location.getPitch() != location.getPitch();
        this.location = location.clone();

        if (sent) sendLocationPacket(viewers);

        return true;
    }


    public void remove() {
        removed = true;
        sendEntityDestroyPacket(viewers);
    }

    public boolean isDead() {
        return removed;
    }

    public boolean isValid() {
        return !removed;
    }

    public void sendSpawnPacket(Collection<Player> players) {
        WrapperPlayServerSpawnEntity spawnEntity = new WrapperPlayServerSpawnEntity(id, uuid, type, SpigotConversionUtil.fromBukkitLocation(location), location.getYaw(), 0, null);
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnEntity));
    }

    public void sendLocationPacket(Collection<Player> players) {

        PacketWrapper<?> packet;
        EntityPositionData data = new EntityPositionData(SpigotConversionUtil.fromBukkitLocation(location).getPosition(), Vector3d.zero(), location.getYaw(), location.getPitch());

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            packet = new WrapperPlayServerEntityPositionSync(id, data, false);
        } else {
            packet = new WrapperPlayServerEntityTeleport(id, data, RelativeFlag.NONE,false);
        }

        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public void sendHeadRotation(Collection<Player> players) {
        WrapperPlayServerEntityRotation packet = new WrapperPlayServerEntityRotation(id, headYaw, headPitch, false);
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public void sendEntityDestroyPacket(Collection<Player> players) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(id);
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public int getEntityId() {
        return id;
    }
}

