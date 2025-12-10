package re.imc.geysermodelengine.runnables;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountController;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.UUID;

public class BedrockMountControlRunnable implements Runnable {

    private final GeyserModelEngine plugin;

    public BedrockMountControlRunnable(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (UUID playerUUID : plugin.getModelManager().getPlayerJoinedCache()) {
            Player player = Bukkit.getPlayer(playerUUID);

            float pitch = player.getLocation().getPitch();
            Pair<ActiveModel, Mount> seat = plugin.getModelManager().getDriversCache().get(player.getUniqueId());
            if (seat == null) continue;

            if (pitch < -30) {
                MountController controller = ModelEngineAPI.getMountPairManager().getController(player.getUniqueId());
                if (controller != null) {
                    MountController.MountInput input = controller.getInput();
                    if (input != null) {
                        input.setJump(true);
                        controller.setInput(input);
                    }
                }
            }

            if (pitch > 80) {
                if (seat.getKey().getModeledEntity().getBase() instanceof BukkitEntity bukkitEntity) {
                    if (bukkitEntity.getOriginal().isOnGround()) continue;
                }

                MountController controller = ModelEngineAPI.getMountPairManager().getController(player.getUniqueId());
                if (controller != null) {
                    MountController.MountInput input = controller.getInput();
                    if (input != null) {
                        input.setSneak(true);
                        controller.setInput(input);
                    }
                }
            }
        }
    }
}
