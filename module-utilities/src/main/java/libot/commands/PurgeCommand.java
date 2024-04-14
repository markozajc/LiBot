package libot.commands;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.core.commands.exceptions.ExceptionHandler.unpackThrowable;
import static libot.core.commands.exceptions.ExceptionHandler.ThrowableHandler.handleThrowable;
import static libot.utils.Utilities.plural;
import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class PurgeCommand extends Command {

	private static final String FORMAT_NEGATIVE_QUANTITY = "The number of messages to purge must be positive.";
	private static final String FORMAT_DONE = "Purged **%d** message%s";

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		int quantity = c.params().getIntOrDefault(0, 1);
		if (0 >= quantity)
			throw c.error(FORMAT_NEGATIVE_QUANTITY, FAILURE);

		c.getChannel().getIterableHistory().takeAsync(quantity + 1).thenApply(l -> {
			c.getChannel().purgeMessages(l);
			return l.size();
		}).thenAccept(n -> c.replyf(FORMAT_DONE, LITHIUM, n - 1, plural(n - 1))).exceptionally(e -> {
			handleThrowable(c, unpackThrowable(e));
			return null;
		});
	}

	@Override
	public String getName() {
		return "purge";
	}

	@Override
	public String getInfo() {
		return "Purges/deletes messages from a channel.";
	}

	@Override
	public Permission[] getPermissions() {
		return new Permission[] { MESSAGE_MANAGE };
	}

	@Override
	public String[] getParameters() {
		return new String[] { "messages" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "number of messages to purge" };
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
