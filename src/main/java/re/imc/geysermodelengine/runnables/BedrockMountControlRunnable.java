package re.imc.geysermodelengine.runnables;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountController;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.function.Consumer;

public class BedrockMountControlRunnable implements Consumer<ScheduledTask> {

    private final GeyserModelEngine plugin;

    public BedrockMountControlRunnable(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void accept(ScheduledTask scheduledTask) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) continue;

            float pitch = player.getLocation().getPitch();
            Pair<ActiveModel, Mount> seat = plugin.getModelManager().getDriversCache().get(player);

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
                    if (bukkitEntity.getOriginal().isOnGround()) {
                        return;
                    }
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
