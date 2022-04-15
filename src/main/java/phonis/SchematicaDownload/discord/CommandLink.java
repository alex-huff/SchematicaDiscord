package phonis.SchematicaDownload.discord;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommandLink extends DiscordCommand
{

    public CommandLink()
    {
        super(
            "link",
            "Link Discord and Minecraft for schematica transfer.",
            null
        );
    }

    @Override
    public void handleCommand(MessageReceivedEvent receivedEvent, String[] args)
    {
        if (!receivedEvent.getChannelType().equals(ChannelType.PRIVATE))
        {
            receivedEvent.getChannel().sendMessage("Check dms.").queue();
        }

        receivedEvent.getAuthor().openPrivateChannel().queue(
            privateChannel -> privateChannel.sendMessage(
                "Execute /pschem link " +
                DiscordManager.createLinkSession(receivedEvent.getAuthor().getIdLong()).toString() + " in game."
            ).queue()
        );
    }

}
