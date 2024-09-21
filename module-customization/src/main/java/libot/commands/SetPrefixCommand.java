package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.CustomizationsProvider;

public class SetPrefixCommand extends Command {

	@Nonnull private static final Parameter PREFIX =
		optional(POSITIONAL, "prefix", "prefix to use (leave empty to reset to default)");

	public SetPrefixCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "setprefix")
			.aliases("prefix")
			.permissions(MANAGE_SERVER)
			.parameters(PREFIX)
			.description("""
				Changes LiBot's command prefix.""")
			.build());
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) throws Exception {
		c.arg(PREFIX).map(Argument::value).ifPresentOrElse(prefix -> {
			change(c, prefix);
		}, () -> {
			reset(c);
		});
		c.react(ACCEPT_EMOJI);
	}

	@SuppressWarnings("null")
	private static void reset(@Nonnull CommandContext c) {
		var prov = c.getProvider(CustomizationsProvider.class);
		if (prov.get(c).getCustomPrefix().isEmpty())
			throw c.error("A custom prefix is not set", DISABLED);

		if (!c.confirmf("Are you sure you want remove custom command prefix and revert it to the default one (%s)?",
						LITHIUM, monospace(c.getConfig().defaultPrefix()))) {
			throw c.cancel();
		}

		prov.get(c).setCommandPrefix(null);
	}

	private static void change(@Nonnull CommandContext c, @Nonnull String prefix) {
		if (prefix.length() > MAX_CUSTOM_PREFIX_LENGTH) {
			throw c.errorf("The maximum length of a prefix is %d characters, but yours is %d characters long!",
						   MAX_CUSTOM_PREFIX_LENGTH, prefix.length());
		}

		if (!c.confirmf("Are you sure you want to change LiBot's command prefix for this guild to %s?", LITHIUM,
						monospace(prefix))) {
			throw c.cancel();
		}

		c.getProvider(CustomizationsProvider.class).get(c.getGuildIdLong()).setCommandPrefix(prefix);
	}

}
