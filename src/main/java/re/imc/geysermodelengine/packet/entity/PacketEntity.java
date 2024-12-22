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

    // public static final MinecraftVersion V1_20_5 = new MinecraftVersion("1.20.5");
    public PacketEntity(EntityType type, Set<Player> viewers, Location location) {
        this.id = ThreadLocalRandom.current().nextInt(300000000, 400000000);
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.viewers = viewers;
        this.location = location;
    }

    private int id;
    private UUID uuid;
    private EntityType type;
    private Set<Player> viewers;
    private Location location;
    private float headYaw;
    private float headPitch;

    private boolean removed = false;
    public @NotNull Location getLocation() {
        return location;
    }

    public boolean teleport(@NotNull Location location) {
        boolean sent = this.location.getWorld() != location.getWorld() || this.location.distanceSquared(location) > 0.000001;
        this.location = location.clone();
        if (sent) {
            sendLocationPacket(viewers);
            // sendHeadRotation(viewers); // TODO
        }
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
        // EntitySpawnPacket packet = new EntitySpawnPacket(id, uuid, type, location);
        // EntityMetadataPacket metadataPacket = new EntityMetadataPacket(id);
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


    /*
    public boolean teleport(@NotNull Location location) {
        this.location = location.clone();
        sendLocationPacket(viewers);
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
        EntitySpawnPacket packet = new EntitySpawnPacket(id, uuid, type, location);
        // EntityMetadataPacket metadataPacket = new EntityMetadataPacket(id);

        players.forEach(player -> {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode());
        });
        sendAllEquipmentPacket(players);
        // players.forEach(player -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadataPacket.encode()));
    }

    public void sendAllEquipmentPacket(Collection<Player> players) {
        for (Map.Entry<EnumWrappers.ItemSlot, ItemStack> e : equipment.entrySet()) {
            EntityEquipmentPacket packet = new EntityEquipmentPacket(id, e.getKey(), e.getValue());

            players.forEach(player -> {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode());
            });
        }
    }

    public void sendLocationPacket(Collection<Player> players) {
        WrapperPacket packet = MinecraftVersion.v1_21_2.atOrAbove() ? new EntityPositionSyncPacket(id, location) : new EntityTeleportPacket(id, location);
        players.forEach(player -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode()));

    }

    public void sendHurtPacket(Collection<Player> players) {
        // 1.21 error
        if (MinecraftVersion.getCurrentVersion().compareTo(V1_20_5) < 0) {
          }
    }
      EntityHurtPacket packet = new EntityHurtPacket(id);
            players.forEach(player -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode()));

    public void sendEntityDestroyPacket(Collection<Player> players) {
        EntityDestroyPacket packet = new EntityDestroyPacket(id);
        players.forEach(player -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode()));
    }

    public int getEntityId() {
        return id;
    }

    public void setSlot(EnumWrappers.ItemSlot slot, ItemStack itemStack) {
        if (itemStack == null) {
            itemStack = new ItemStack(Material.AIR);
        }
        equipment.put(slot, itemStack);
        EntityEquipmentPacket packet = new EntityEquipmentPacket(id, slot, itemStack);
        viewers.forEach(player -> {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode());
        });
    }

     */
}

