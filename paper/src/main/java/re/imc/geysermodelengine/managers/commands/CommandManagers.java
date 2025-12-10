package re.imc.geysermodelengine.managers.commands;

import re.imc.geysermodelengine.managers.commands.subcommands.SubCommands;

import java.util.ArrayList;

public interface CommandManagers {

    /**
     * Gets the name of the command manager
     */
    String getName();

    /**
     * Gets the command manager subcommands
     */
    ArrayList<SubCommands> getCommands();
}
