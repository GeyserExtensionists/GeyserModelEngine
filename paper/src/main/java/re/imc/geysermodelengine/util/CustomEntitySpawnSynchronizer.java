package re.imc.geysermodelengine.util;

import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class CustomEntitySpawnSynchronizer {


    public static void sendAndSpawn(GeyserModelEngine plugin, Player player, int entityId, String identifier, int requestedDelay, Runnable spawnAction) {
        long syncDelay = Math.max(requestedDelay, plugin.getConfigManager().getConfig().getInt("models.custom-entity-sync-delay", 150));
        long resendInterval = Math.max(1L, plugin.getConfigManager().getConfig().getInt("models.custom-entity-sync-resend-interval", 40));
        int resendCount = Math.max(1, plugin.getConfigManager().getConfig().getInt("models.custom-entity-sync-resend-count", 3));

        Runnable sendMapping = () -> {
            if (player.isOnline()) {
                EntityUtils.setCustomEntity(player, entityId, identifier);
            }
        };

        Bukkit.getScheduler().runTask(plugin, sendMapping);

        Set<Long> scheduled = new HashSet<>();
        for (int i = 1; i < resendCount; i++) {
            long resendDelay = Math.min(syncDelay - 1L, resendInterval * i);
            if (resendDelay <= 0L || !scheduled.add(resendDelay)) {
                continue;
            }
            plugin.getSchedulerPool().schedule(() -> Bukkit.getScheduler().runTask(plugin, sendMapping), resendDelay, TimeUnit.MILLISECONDS);
        }

        plugin.getSchedulerPool().schedule(() -> {
            if (player.isOnline()) {
                spawnAction.run();
            }
        }, syncDelay, TimeUnit.MILLISECONDS);
    }
}
