package phonis.SchematicaDownload.commands;

import org.bukkit.ChatColor;

public class CommandException extends Exception
{

    public static final  CommandException consoleError = new CommandException("Only players can use this command");
    private static final String           prefix       = ChatColor.RED + "SchematicaDownload command usage error " +
                                                         ChatColor.GRAY + "➤ " + ChatColor.WHITE;

    public CommandException(String error)
    {
        super(prefix + error);
    }

}
