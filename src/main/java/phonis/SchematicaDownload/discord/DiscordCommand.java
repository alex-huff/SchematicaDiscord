package phonis.SchematicaDownload.discord;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashSet;
import java.util.Set;

public abstract class DiscordCommand
{

    private   String                  name;
    private   String                  hint;
    private   String                  args;
    private   int                     minArgLen;
    private   int                     maxArgLen;
    private   Set<String>             aliases = new HashSet<>();
    protected DiscordCommandException argumentException;

    public DiscordCommand(String name, String hint, String args, int minArgLen, int maxArgLen)
    {
        this.name              = name.toLowerCase();
        this.hint              = hint;
        this.args              = args;
        this.minArgLen         = minArgLen;
        this.maxArgLen         = maxArgLen == -1 ? Integer.MAX_VALUE : maxArgLen;
        this.argumentException = new DiscordCommandException(this.argumentError());
    }

    public DiscordCommand(String name, String hint, String args)
    {
        this(name, hint, args, 0, -1);
    }

    public String getName()
    {
        return this.name;
    }

    public String getHint()
    {
        return this.hint;
    }

    public String getArgs()
    {
        return this.args;
    }

    public void onCommand(MessageReceivedEvent receivedEvent, String[] args)
    {
        try
        {
            this.checkArgumentSize(args);
            this.handleCommand(receivedEvent, args);
        }
        catch (DiscordCommandException dce)
        {
            receivedEvent.getChannel().sendMessage(dce.getMessage()).queue();
        }
    }

    private void checkArgumentSize(String[] args) throws DiscordCommandException
    {
        if (args.length < this.minArgLen || args.length > this.maxArgLen)
        {
            throw this.argumentException;
        }
    }

    public abstract void handleCommand(MessageReceivedEvent receivedEvent, String[] args)
        throws DiscordCommandException;

    public void addAlias(String alias)
    {
        this.aliases.add(alias.toLowerCase());
    }

    public boolean isCommand(String rawName)
    {
        String lowerName = rawName.toLowerCase();

        return lowerName.equals(this.name) || this.aliases.contains(lowerName);
    }

    protected int parseInt(String string, int min, int max) throws DiscordCommandException
    {
        try
        {
            int num = Integer.parseInt(string);

            if (num < min || num > max)
            {
                throw new DiscordCommandException("Number should be between " + min + " and " + max + ".");
            }

            return num;
        }
        catch (NumberFormatException ignored)
        {
            throw this.argumentException;
        }
    }

    protected long parseLong(String string, long min, long max) throws DiscordCommandException
    {
        try
        {
            long num = Long.parseLong(string);

            if (num < min || num > max)
            {
                throw new DiscordCommandException("Number should be between " + min + " and " + max + ".");
            }

            return num;
        }
        catch (NumberFormatException ignored)
        {
            throw this.argumentException;
        }
    }

    protected long parseLong(String string) throws DiscordCommandException
    {
        return this.parseLong(string, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    protected int parseInt(String string) throws DiscordCommandException
    {
        return this.parseInt(string, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    protected void assertMemberPermission(Member member, Permission permission) throws DiscordCommandException
    {
        if (!member.hasPermission(permission))
        {
            throw new DiscordPermissionException(permission);
        }
    }

    protected void assertMemberPermission(Member member, GuildChannel channel, Permission permission)
        throws DiscordCommandException
    {
        if (!member.hasPermission(channel, permission))
        {
            throw new DiscordPermissionException(permission);
        }
    }

    protected void assertBool(boolean bool, String message) throws DiscordCommandException
    {
        if (!bool)
        {
            throw new DiscordCommandException(message);
        }
    }

    protected <E extends Object> E assertNonNullArgument(E element) throws DiscordCommandException
    {
        return this.assertNonNull(element, this.argumentException);
    }

    protected <E extends Object> E assertNonNull(E element, DiscordCommandException exception)
        throws DiscordCommandException
    {
        if (element == null)
        {
            throw exception;
        }

        return element;
    }

    protected <E extends Object> E assertNonNull(E element, String message) throws DiscordCommandException
    {
        return this.assertNonNull(element, new DiscordCommandException(message));
    }

    protected void assertInGuild(MessageReceivedEvent receivedEvent) throws DiscordCommandException
    {
        if (receivedEvent.getMember() == null)
        {
            throw DiscordCommandException.notInGuild;
        }
    }

    protected String argumentError()
    {
        return "Argument error. Usage: " + this.args;
    }

    protected Set<String> getAliases()
    {
        return this.aliases;
    }

    public static class DiscordCommandException extends Exception
    {

        public static DiscordCommandException invalidPlatform = new DiscordCommandException("Invalid platform.");
        public static DiscordCommandException notInGuild      = new DiscordCommandException(
            "Use this command in a guild.");

        public DiscordCommandException(String message)
        {
            super(message);
        }

    }

    public static class DiscordPermissionException extends DiscordCommandException
    {

        public DiscordPermissionException(Permission permission)
        {
            super("You need " + permission.getName().toLowerCase() + " permission to use this command.");
        }

    }

}