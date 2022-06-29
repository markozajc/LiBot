package libot.commands;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.CustomizationsProvider;
import net.dv8tion.jda.api.Permission;

public class SetPrefixCommand extends Command {

	private static final String FORMAT_LENGTH_EXCEEDED = """
		The maximum length of a prefix is %d characters, but yours is %d characters long!""";
	private static final String FORMAT_CONFIRM_CHANGE = """
		Are you sure you want to change LiBot's command prefix for this guild to %s?""";
	private static final String FORMAT_CONFIRM_RESET = """
		Are you sure you want remove custom command prefix and revert it to the default one (%s)?""";
	private static final String FORMAT_NO_PREFIX = """
		A custom prefix is not set""";

	@Override
	public void execute(CommandContext c) throws Exception {
		if (c.params().check(0))
			change(c);
		else
			reset(c);
		c.react(ACCEPT_EMOJI);
	}

	private static void reset(@Nonnull CommandContext c) {
		var prov = c.provider(CustomizationsProvider.class);
		if (prov.get(c).getCustomPrefix().isEmpty())
			throw c.error(FORMAT_NO_PREFIX, DISABLED);

		if (!c.confirmf(FORMAT_CONFIRM_RESET, LITHIUM, monospace(c.getConfig().defaultPrefix())))
			throw c.cancel();

		prov.get(c).setCommandPrefix(null);
	}

	private static void change(@Nonnull CommandContext c) {
		var prefix = c.params().get(0);
		if (prefix.length() > MAX_CUSTOM_PREFIX_LENGTH)
			throw c.errorf(FORMAT_LENGTH_EXCEEDED, MAX_CUSTOM_PREFIX_LENGTH, prefix.length());

		if (!c.confirmf(FORMAT_CONFIRM_CHANGE, LITHIUM, monospace(prefix)))
			throw c.cancel();

		c.provider(CustomizationsProvider.class).get(c.getGuildIdLong()).setCommandPrefix(prefix);
	}

	@Override
	public String getName() {
		return "setprefix";
	}

	@Override
	public String[] getAliases() {
		return array("prefix");
	}

	@Override
	public String getInfo() {
		return """
			Changes LiBot's command prefix.""";
	}

	@Override
	public Permission[] getPermissions() {
		return array(MANAGE_SERVER);
	}

	@Override
	public String[] getParameters() {
		return array("[prefix]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("prefix to use (leave empty to reset to default)");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return CUSTOMIZATION;
	}
}
