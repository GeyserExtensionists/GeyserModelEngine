package re.imc.geysermodelengine.model;

import com.comphenix.protocol.wrappers.Pair;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountController;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import re.imc.geysermodelengine.GeyserModelEngine;

public class BedrockMountControl {

    public static void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    float pitch = player.getPitch();
                    Pair<ActiveModel, Mount> seat = GeyserModelEngine.getInstance().getDrivers().get(player);
                    if (seat != null) {
                        if (pitch < -30) {
                            MountController controller = ModelEngineAPI.getMountPairManager()
                                    .getController(player.getUniqueId());
                            if (controller != null) {
                                MountController.MountInput input = controller.getInput();
                                if (input != null) {
                                    input.setJump(true);
                                    controller.setInput(input);
                                }
                            }
                        }
                        if (pitch > 50) {
                            MountController controller = ModelEngineAPI.getMountPairManager()
                                    .getController(player.getUniqueId());
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
        }.runTaskTimerAsynchronously(GeyserModelEngine.getInstance(), 1,  1);
    }
}
