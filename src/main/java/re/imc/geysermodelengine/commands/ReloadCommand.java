package re.imc.geysermodelengine.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import re.imc.geysermodelengine.GeyserModelEngine;

public class ReloadCommand implements CommandExecutor {

    private final GeyserModelEngine plugin;

    public ReloadCommand(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player && !sender.hasPermission("geysermodelengine.reload")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        plugin.reloadConfig();
        plugin.reload();

        sender.sendMessage("§aGeyserModelEngine configuration reloaded!");
        return true;
    }
}