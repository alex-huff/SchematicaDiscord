package phonis.SchematicaDownload.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import phonis.SchematicaDownload.discord.DiscordManager;

import java.util.List;
import java.util.UUID;

public class CommandLink extends SubCommand
{

    public CommandLink()
    {
        super("link", "(UUID)");
    }

    @Override
    public List<String> topTabComplete(CommandSender sender, String[] args)
    {
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) throws CommandException
    {
        throw CommandException.consoleError;
    }

    @Override
    public void execute(Player player, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            player.sendMessage(this.getCommandString(0));

            return;
        }

        UUID linkUUID;

        try
        {
            linkUUID = UUID.fromString(args[0]);
        }
        catch (IllegalArgumentException e)
        {
            throw new CommandException("Illegal UUID.");
        }

        if (DiscordManager.link(player.getUniqueId(), linkUUID))
        {
            player.sendMessage("Account linked");
        }
        else
        {
            throw new CommandException("Invalid link UUID");
        }
    }

}
