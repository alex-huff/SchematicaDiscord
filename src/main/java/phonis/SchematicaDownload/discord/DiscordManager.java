package phonis.SchematicaDownload.discord;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import phonis.SchematicaDownload.util.ClipboardConverter;
import phonis.SchematicaDownload.util.ClipboardException;
import phonis.SchematicaDownload.util.PicStreamToClipboard;
import phonis.SchematicaDownload.util.SchemStreamToClipboard;

import javax.security.auth.login.LoginException;
import java.io.InputStream;
import java.util.*;

public class DiscordManager extends ListenerAdapter {

    public List<DiscordCommand> commands;
    private static List<GatewayIntent> intents;
    private static Map<UUID, Long> linkSession;
    private static BidiMap<UUID, Long> minecraftDiscordMap;

    static {
        intents = new ArrayList<>();

        intents.add(GatewayIntent.DIRECT_MESSAGES);
        intents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS);
        intents.add(GatewayIntent.DIRECT_MESSAGE_TYPING);
        intents.add(GatewayIntent.GUILD_BANS);
        intents.add(GatewayIntent.GUILD_EMOJIS);
        intents.add(GatewayIntent.GUILD_INVITES);
        intents.add(GatewayIntent.GUILD_VOICE_STATES);
        intents.add(GatewayIntent.GUILD_MESSAGES);
        intents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS);
        intents.add(GatewayIntent.GUILD_MESSAGE_TYPING);
        //intents.add(GatewayIntent.GUILD_PRESENCES);
        //intents.add(GatewayIntent.GUILD_MEMBERS);

        linkSession = new HashMap<>();
        minecraftDiscordMap = new DualHashBidiMap<>();
    }

    public JDA jda;
    private JavaPlugin plugin;

    public DiscordManager(String token, JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new ArrayList<>();

        this.commands.add(new CommandHelp(this.commands));
        this.commands.add(new CommandLink());

        try {
            this.jda = JDABuilder.create(token, intents).setActivity(
                EntityBuilder.createActivity("!help", null, Activity.ActivityType.DEFAULT)
            ).disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS).build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        this.jda.addEventListener(this);
    }

    public synchronized static UUID getMinecraftFromDiscord(Long id) {
        return DiscordManager.minecraftDiscordMap.inverseBidiMap().get(id);
    }

    public synchronized static Long getDiscordFromMinecraft(UUID uuid) {
        return DiscordManager.minecraftDiscordMap.get(uuid);
    }

    public synchronized static boolean link(UUID minecraftUUID, UUID linkSessionUUID) {
        Long dID = DiscordManager.linkSession.get(linkSessionUUID);

        if (dID == null) {
            return false;
        }

        DiscordManager.linkSession.remove(linkSessionUUID);
        DiscordManager.minecraftDiscordMap.put(minecraftUUID, dID);

        return true;
    }

    public synchronized static UUID createLinkSession(long id) {
        UUID uuid = UUID.randomUUID();

        DiscordManager.linkSession.put(uuid, id);

        return uuid;
    }

    public synchronized static void putLink(UUID minecraftUUID, Long dID) {
        DiscordManager.minecraftDiscordMap.put(minecraftUUID, dID);
    }

    public synchronized static Set<Map.Entry<UUID, Long>> getLinks() {
        return DiscordManager.minecraftDiscordMap.entrySet();
    }

    private void handleCommand(String command, String[] args, MessageReceivedEvent receivedEvent) {
        for (DiscordCommand discordCommand : this.commands) {
            if (discordCommand.isCommand(command)) {
                discordCommand.onCommand(receivedEvent, args);

                return;
            }
        }
    }

    public static String[] truncate(String[] strings) {
        String[] ret = new String[strings.length - 1];

        System.arraycopy(strings, 1, ret, 0, strings.length - 1);

        return ret;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == this.jda.getSelfUser().getIdLong()) {
            return;
        }

        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!")) {
            String[] split = message.split(" ");

            System.out.println(event.getAuthor().getName() + ": " + message);
            handleCommand(split[0].substring(1), DiscordManager.truncate(split), event);

            return;
        }

        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        if (!event.getChannelType().equals(ChannelType.PRIVATE) || attachments.size() != 1) {
            return;
        }

        Message.Attachment attachment = attachments.get(0);
        ClipboardConverter converter;

        if (attachment.getFileName().endsWith(".schematic")) {
            converter = new SchemStreamToClipboard();
        } else if (attachment.getFileName().endsWith(".png") || attachment.getFileName().endsWith(".jpg")) {
            converter = new PicStreamToClipboard(event.getMessage().getContentRaw());
        } else {
            return;
        }

        this.handleUpload(event, attachment, converter);
    }

    private void handleUpload(MessageReceivedEvent event, Message.Attachment attachment, ClipboardConverter converter) {
        UUID playerUUID = DiscordManager.getMinecraftFromDiscord(event.getMessage().getAuthor().getIdLong());

        if (playerUUID == null) {
            event.getChannel().sendMessage("Link your Minecraft account using !link.").queue();

            return;
        }

        attachment.retrieveInputStream().thenAccept(
            stream -> Bukkit.getScheduler().scheduleSyncDelayedTask(
                this.plugin,
                () -> {
                    try {
                        this.loadClipboard(event, converter, stream, playerUUID);
                    } catch (ClipboardException e) {
                        event.getChannel().sendMessage(e.getMessage()).queue();
                    }
                }
            )
        );
    }

    private void loadClipboard(MessageReceivedEvent event, ClipboardConverter converter, InputStream stream, UUID playerUUID) throws ClipboardException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        WorldEditPlugin wep;

        if (plugin instanceof WorldEditPlugin) {
            wep = (WorldEditPlugin) plugin;
        } else {
            throw new ClipboardException("Invalid WorldEdit.");
        }

        Player player = Bukkit.getPlayer(playerUUID);

        if (player == null) {
            throw new ClipboardException("You are not online in Minecraft.");
        }

        Clipboard clipboard = converter.getClipboard(stream);
        LocalSession session = wep.getSession(player);
        String dims = clipboard.getDimensions().toString();

        session.setClipboard(new ClipboardHolder(clipboard));
        player.sendMessage(ChatColor.GREEN + "Loaded to clipboard. " + dims);
        event.getChannel().sendMessage("Loaded to clipboard. " + dims).queue();

        if (converter instanceof PicStreamToClipboard) {
            event.getChannel().sendMessage("Size a picture by commenting (width) (height) when you upload.").queue();
        }
    }

    public static MessageEmbed embed(String title, String description, MessageEmbed.ImageInfo imageInfo, List<MessageEmbed.Field> fields) {
        return new MessageEmbed(
            null,
            title,
            description,
            EmbedType.RICH,
            null,
            1238,
            null,
            null,
            null,
            null,
            new MessageEmbed.Footer(
                "SchemDownload",
                "http://ddragon.leagueoflegends.com/cdn/10.19.1/img/champion/Jinx.png",
                null
            ),
            imageInfo,
            fields
        );
    }

    public static String inlineCodeBlock(String string) {
        return "`" + string + "`";
    }

    public static String inlineCodeBlock(String[] strings) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String str : strings) {
            stringBuilder.append(inlineCodeBlock(str)).append(" ");
        }

        return stringBuilder.toString();
    }

}
