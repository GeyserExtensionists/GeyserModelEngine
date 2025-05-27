package re.imc.geysermodelengine.managers.server;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.ConcurrentHashMap;

public class ServerData {

    private final ConcurrentHashMap<String, ScheduledTask> activeRunnablesCache = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, ScheduledTask> getActiveRunnablesCache() {
        return activeRunnablesCache;
    }
}