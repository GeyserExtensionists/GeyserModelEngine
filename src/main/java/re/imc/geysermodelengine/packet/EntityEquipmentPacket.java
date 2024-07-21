package re.imc.geysermodelengine.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

public class EntityEquipmentPacket implements WrapperPacket {

    private final int id;
    private final EnumWrappers.ItemSlot slot;
    private final ItemStack itemStack;
    public EntityEquipmentPacket(int id, EnumWrappers.ItemSlot slot, ItemStack itemStack) {
        this.id = id;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    @Override
    public PacketContainer encode() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().writeSafely(0, id);
        packet.getItemSlots().writeSafely(0, slot);
        packet.getItemModifier().writeSafely(0, itemStack);
        return packet;
    }
}
