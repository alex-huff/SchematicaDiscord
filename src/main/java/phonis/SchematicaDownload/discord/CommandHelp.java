package phonis.SchematicaDownload.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class CommandHelp extends DiscordCommand {

    private List<DiscordCommand> commands;

    public CommandHelp(List<DiscordCommand> commands) {
        super(
            "help",
            "List all the commands with info about them.",
            null
        );

        this.commands = commands;

        this.addAlias("h");
    }

    @Override
    public void handleCommand(MessageReceivedEvent receivedEvent, String[] args) {
        List<MessageEmbed.Field> fields = new ArrayList<>();

        for (DiscordCommand command : this.commands) {
            StringBuilder infoBuilder = new StringBuilder();

            infoBuilder
                .append(command.getHint()).append("\n");

            if (command.getArgs() != null) {
                infoBuilder.append("Args: ").append(DiscordManager.inlineCodeBlock(command.getArgs().split(" "))).append("\n");
            }

            if (command.getAliases().size() > 0) {
                infoBuilder.append("Aliases: ");

                for (String alias : command.getAliases()) {
                    infoBuilder.append(DiscordManager.inlineCodeBlock(alias)).append(" ");
                }
            }

            MessageEmbed.Field field = new MessageEmbed.Field(
                "!" + command.getName(),
                infoBuilder.toString(),
                false,
                false
            );

            fields.add(field);
        }

        receivedEvent.getChannel().sendMessage(
            DiscordManager.embed(
                "Commands",
                null,
                null,
                fields
            )
        ).queue();
    }

}
