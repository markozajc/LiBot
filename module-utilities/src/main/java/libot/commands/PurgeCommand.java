package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.plural;
import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.commands.exceptions.ExceptionHandler;
import libot.core.entities.CommandContext;

public class PurgeCommand extends Command {

	@Nonnull private static final MandatoryParameter COUNT =
		mandatory(POSITIONAL, "count", "number of messages to purge");

	public PurgeCommand() {
		super(CommandMetadata.builder(UTILITIES, "purge")
			.aliases("prune")
			.permissions(MESSAGE_MANAGE)
			.parameters(COUNT)
			.description("Purges/deletes messages from a channel."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		int quantity = c.arg(COUNT).valueAsInt();
		if (0 >= quantity)
			throw c.error("The number of messages to purge must not be negative.", FAILURE);

		c.getChannel().getIterableHistory().takeAsync(quantity + 1).thenApply(l -> {
			c.getChannel().purgeMessages(l);
			return l.size();
		}).thenAccept(n -> c.replyf("Purged **%d** message%s", LITHIUM, n - 1, plural(n - 1))).exceptionally(e -> {
			ExceptionHandler.handle(e, c);
			return null;
		});
	}

}
