package re.imc.geysermodelengine.managers.commands;

import re.imc.geysermodelengine.managers.commands.subcommands.SubCommands;

import java.util.ArrayList;

public interface CommandManagers {

    String name();

    ArrayList<SubCommands> getCommands();
}
