package phonis.SchematicaDownload.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CommandPSchem extends SubCommand {

    public CommandPSchem(JavaPlugin plugin) {
        super("pschem");
        SubCommand.registerCommand(plugin, this);
        this.addSubCommand(new CommandSave(plugin));
        this.addSubCommand(new CommandLink());
    }

    @Override
    public List<String> topTabComplete(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(this.getCommandString(0));
    }

    @Override
    public void execute(Player player, String[] args) {
        this.execute((CommandSender) player, args);
    }

}
