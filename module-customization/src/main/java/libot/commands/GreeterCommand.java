package libot.commands;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.listeners.GreeterListener;
import libot.providers.GreeterProvider;
import libot.providers.GreeterProvider.GreeterConfiguration;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class GreeterCommand extends Command {

	private static final String FORMAT_INVALID_TYPE = """
		%s is not a valid event type. Please use `WELCOME` or `GOODBYE`.""";
	private static final String FORMAT_INVALID_SUBCOMMAND = """
		%s is not a valid subcommand. Please use `set`, `remove`, or `test`.""";
	private static final String FORMAT_SET = """
		You're setting the **%s** message for %s.
		Please type in your desired message.""";
	private static final String FORMAT_SET_VARIABLES_LIST = """
		`{name}   ` Username - %s
		`{discrim}` User's discriminator - %s
		`{ping}   ` User mention - %s
		`{guild}  ` Guild name - %s""";
	private static final String FORMAT_SET_TOO_LONG = "Message length may not exceed %d characters.";
	private static final String FORMAT_SET_OK = """
		Successfully set the greeter for %s. You may now test it with `%sgreeter test %s`.""";
	private static final String FORMAT_REMOVE_CONFIRM = """
		Are you sure you want to remove the %s message?""";
	private static final String FORMAT_TEST_MISSING = """
		A %s message has not yet been configured for this guild.""";
	private static final String FORMAT_TEST_CHANNEL_MISSING = """
		The channel LiBot would send the welcome/goodbye message to does no longer exist.
		Do you want to set %s as the welcome/goodbye channel?""";
	private static final String FORMAT_TEST_OK = """
		A test %s message has been sent to %s.""";

	private enum EventType {
		WELCOME,
		GOODBYE
	}

	@Override
	public void execute(CommandContext c) {
		String ets = c.params().get(1).toLowerCase();
		var type = switch (ets) {
			case "welcome" -> EventType.WELCOME;
			case "goodbye" -> EventType.GOODBYE;
			default -> throw c.errorf(FORMAT_INVALID_TYPE, FAILURE, ets);
		};

		String scs = c.params().get(0).toLowerCase();
		var greeter = c.provider(GreeterProvider.class).get(c.getGuildIdLong());
		switch (scs) {
			case "set" -> set(c, type, greeter);
			case "remove" -> remove(c, type, greeter);
			case "test" -> test(c, type, greeter);
			default -> throw c.errorf(FORMAT_INVALID_SUBCOMMAND, FAILURE, scs);
		}
	}

	public static void set(@Nonnull CommandContext c, @Nonnull EventType type, @Nonnull GreeterConfiguration conf) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setDescriptionf(FORMAT_SET, type.toString().toLowerCase(), c.getChannelMention());
		e.addFieldf("Available variables", FORMAT_SET_VARIABLES_LIST, c.getUsername(), c.getUserDiscriminator(),
					c.getUserMention(), c.getGuildName());
		e.setFooter(EXIT_FOOTER);

		var input = c.askraw(e).getContentRaw();
		if ("exit".equalsIgnoreCase(input))
			return;
		if (input.length() > MAX_GREETER_MESSAGE_LENGTH)
			throw c.errorf(FORMAT_SET_TOO_LONG, FAILURE, MAX_GREETER_MESSAGE_LENGTH);

		conf.setChannel(c.getChannelIdLong(), c.getChannelType());

		switch (type) {
			case WELCOME -> conf.setWelcomeMessage(input);
			case GOODBYE -> conf.setGoodbyeMessage(input);
		}

		c.replyf(FORMAT_SET_OK, SUCCESS, c.getChannelMention(), c.getEffectivePrefix(), type.toString().toLowerCase());
	}

	public static void remove(@Nonnull CommandContext c, @Nonnull EventType type, @Nonnull GreeterConfiguration conf) {
		if (!c.confirmf(FORMAT_REMOVE_CONFIRM, type.toString().toLowerCase()))
			return;

		String message = switch (type) {
			case WELCOME -> conf.getWelcomeMessage();
			case GOODBYE -> conf.getGoodbyeMessage();
		};

		if (message == null)
			throw c.errorf(FORMAT_TEST_MISSING, DISABLED, type.toString().toLowerCase());

		switch (type) {
			case WELCOME -> conf.setWelcomeMessage(null);
			case GOODBYE -> conf.setGoodbyeMessage(null);
		}

		c.reply("Message removed successfully.", SUCCESS);
	}

	public static void test(@Nonnull CommandContext c, @Nonnull EventType type, @Nonnull GreeterConfiguration conf) {
		var message = switch (type) {
			case WELCOME -> conf.getWelcomeMessage();
			case GOODBYE -> conf.getGoodbyeMessage();
		};

		if (message == null)
			throw c.errorf(FORMAT_TEST_MISSING, DISABLED, type.toString().toLowerCase());

		var ch = (MessageChannelUnion) c.getGuild()
			.getChannelCache()
			.getElementById(conf.getChannelType(), conf.getChannelId());

		if (ch == null) {
			if (c.confirmf("// WARNING //", FORMAT_TEST_CHANNEL_MISSING, WARN, c.getChannel().getAsMention())) {
				ch = c.getChannel();
				conf.setChannel(ch.getIdLong(), ch.getType());
			} else {
				throw c.cancel();
			}
		}

		ch.sendMessage(GreeterListener.parseMessage(message, c.getUser(), c.getGuild())).queue();
		c.replyf(FORMAT_TEST_OK, SUCCESS, type.toString().toLowerCase(), ch.getAsMention());
	}

	@Override
	public String getName() {
		return "greeter";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "welcome", "goodbye", "farewell", "wgmo", "welcomegoodbyemessageoptions", "greet" };
	}

	@Override
	public String getInfo() {
		return """
			Allows you to create, test, or remove a goodbye/welcome message for members joining/leaving your guild.
			Subcommands:
			- `set   ` sets the message that will be sent for the welcome/goodbye event
			- `remove` removes the message
			- `test  ` tests the event message by sending it as if you were joining/leaving""";
	}

	@Override
	public Permission[] getPermissions() {
		return new Permission[] { MANAGE_SERVER };
	}

	@Override
	public String[] getParameters() {
		return new String[] { "subcommand", "type" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "set/remove/test", "welcome/goodbye" };
	}

	@Override
	public CommandCategory getCategory() {
		return CUSTOMIZATION;
	}

}
