package re.imc.geysermodelengine.managers.commands.managers.geysermodelengine;

import dev.jorel.commandapi.CommandAPICommand;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.commands.geysermodelenginecommands.GeyserModelEngineReloadCommand;
import re.imc.geysermodelengine.managers.commands.CommandManagers;
import re.imc.geysermodelengine.managers.commands.subcommands.SubCommands;

import java.util.ArrayList;

public class GeyserModelEngineCommandManager implements CommandManagers {

    private final ArrayList<SubCommands> commands = new ArrayList<>();

    public GeyserModelEngineCommandManager(GeyserModelEngine plugin) {
        commands.add(new GeyserModelEngineReloadCommand(plugin));

        registerCommand();
    }

    private void registerCommand() {
        CommandAPICommand geyserModelEngineCommand = new CommandAPICommand(getName());

        commands.forEach(subCommands -> geyserModelEngineCommand.withSubcommand(subCommands.onCommand()));

        geyserModelEngineCommand.register();
    }

    @Override
    public String getName() {
        return "geysermodelengine";
    }

    @Override
    public ArrayList<SubCommands> getCommands() {
        return commands;
    }
}
