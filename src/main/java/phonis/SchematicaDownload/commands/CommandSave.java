package phonis.SchematicaDownload.commands;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import net.dv8tion.jda.api.JDA;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import phonis.SchematicaDownload.SchematicaDownload;
import phonis.SchematicaDownload.discord.DiscordManager;

import java.io.*;
import java.util.List;

public class CommandSave extends SubCommand {

    private JDA jda;
    private boolean enabled = true;

    public CommandSave(JavaPlugin plugin) {
        super(
            "save",
            "(Name)"
        );

        SchematicaDownload di = null;

        if (plugin instanceof SchematicaDownload) {
            di = (SchematicaDownload) plugin;
        }

        if (di == null || di.dm == null || di.dm.jda == null) {
            this.enabled = false;
        } else {
            this.jda = di.dm.jda;
        }
    }

    @Override
    public List<String> topTabComplete(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) throws CommandException {
        throw CommandException.consoleError;
    }

    @Override
    public void execute(Player player, String[] args) throws CommandException {
        if (!this.enabled) {
            player.sendMessage("Discord not loaded.");

            return;
        }

        if (args.length < 1) {
            player.sendMessage(this.getCommandString(0));

            return;
        }

        Long dID = DiscordManager.getDiscordFromMinecraft(player.getUniqueId());

        if (dID == null) {
            throw new CommandException("You must link your Discord to Minecraft first.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        WorldEditPlugin wep;

        if (plugin instanceof WorldEditPlugin) {
            wep = (WorldEditPlugin) plugin;
        } else {
            throw new CommandException("Invalid WorldEdit.");
        }

        LocalSession session = wep.getSession(player);
        ClipboardWriter writer;

        try {
            writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(baos);
        } catch (IOException e) {
            throw new CommandException("IOException creating schematica.");
        }

        Clipboard clipboard;

        try {
            clipboard = session.getClipboard().getClipboard();
        } catch (EmptyClipboardException e) {
            throw new CommandException("Empty clipboard.");
        }

        try {
            writer.write(clipboard);
            writer.close();
        } catch (IOException e) {
            throw new CommandException("IOException saving schematica.");
        }

        try {
            this.jda.retrieveUserById(dID).queue(
                user -> user.openPrivateChannel().queue(
                    privateChannel -> privateChannel.sendFile(new ByteArrayInputStream(baos.toByteArray()), args[0] + ".schem").queue()
                )
            );
        } catch (NullPointerException e) {
            throw new CommandException("Invalid Discord or not in a server with the bot. Consider re-linking");
        }

        player.sendMessage("Sending schematica to your Discord.");
    }

}
