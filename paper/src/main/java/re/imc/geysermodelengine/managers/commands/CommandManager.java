package re.imc.geysermodelengine.managers.commands;

import org.reflections.Reflections;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class CommandManager {

    private final GeyserModelEngine plugin;

    private final HashMap<String, CommandManagers> commandManagersCache = new HashMap<>();

    public CommandManager(GeyserModelEngine plugin) {
        this.plugin = plugin;
        load("re.imc.geysermodelengine.managers.commands.managers");
    }

    private void load(String path) {
        for (Class<?> clazz : new Reflections(path).getSubTypesOf(CommandManagers.class)) {
            try {
                CommandManagers commandManager = (CommandManagers) clazz.getDeclaredConstructor(GeyserModelEngine.class).newInstance(plugin);
                plugin.getLogger().info("Loading Command Manager - " + commandManager.getName());
                commandManagersCache.put(commandManager.getName(), commandManager);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException err) {
                plugin.getLogger().severe("Failed to load Command Manager " + clazz.getName());
                throw new RuntimeException(err);
            }
        }
    }

    public HashMap<String, CommandManagers> getCommandManagersCache() {
        return commandManagersCache;
    }
}
