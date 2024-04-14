package libot.commands;

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.core.FinderUtils.*;
import static libot.core.commands.CommandCategory.MONEY;
import static libot.providers.MoneyProvider.DEFAULT_BALANCE;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.MoneyProvider;

public class TransferCommand extends Command {

	private static final String FORMAT_ILLEGAL_TRANSFER = """
		You must not have less than **%dŁ** after the transfer; you %s.""";
	private static final String FORMAT_SUCCESS = """
		%dŁ has been transferred to %s's wallet""";
	private static final String FORMAT_NEGATIVE_AMOUNT = """
		The amount must be positive""";
	private static final String FORMAT_IS_SELF = """
		You can not transfer Ł to yourself""";
	private static final String FORMAT_IS_BOT = """
		You can not transfer Ł to a bot""";
	private static final String FORMAT_INSUFFICIENT = """
		Insufficient funds! Check your balance with `%smoney`""";
	private static final String FORMAT_CONFIRM = """
		Are you sure you want to transfer **%dŁ** to **%s**?""";
	private static final String FORMAT_USER_NOT_FOUND = """
		User "%s" was not found""";

	@Override
	public void execute(CommandContext c) {
		var targets = preferFromGuild(c, findUsers(c, c.params().get(1)));
		if (targets.isEmpty())
			throw c.errorf(FORMAT_USER_NOT_FOUND, FAILURE, escape(c.params().get(1)));

		int amount = c.params().getInt(0);
		if (amount < 1)
			throw c.error(FORMAT_NEGATIVE_AMOUNT, FAILURE);

		var recepient = targets.get(0);
		if (c.getUser().equals(recepient))
			throw c.error(FORMAT_IS_SELF, DISABLED);

		if (recepient.isBot())
			throw c.error(FORMAT_IS_BOT, DISABLED);

		var provider = c.provider(MoneyProvider.class);
		if (amount > provider.getBalance(c.getUserIdLong()))
			throw c.errorf(FORMAT_INSUFFICIENT, FAILURE, c.getEffectivePrefix());

		long maxTransfer = provider.getBalance(c.getUserIdLong()) - DEFAULT_BALANCE;
		if (amount > maxTransfer) {
			throw c.errorf(FORMAT_ILLEGAL_TRANSFER, FAILURE, DEFAULT_BALANCE, maxTransfer <= 0 ? "can't transfer any Ł"
				: format("can transfer at most **%dŁ**", maxTransfer));
		}

		boolean confirm = c.confirmf(FORMAT_CONFIRM, LITHIUM, amount, escape(recepient.getAsTag()));
		if (confirm) {
			provider.takeMoney(c.getUserIdLong(), amount);
			provider.addMoney(recepient.getIdLong(), amount);

			c.replyf("Transfer successful", FORMAT_SUCCESS, SUCCESS, amount, escape(recepient.getAsTag()));
		}
	}

	@Override
	public String getName() {
		return "transfer";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "sendmoney", "donate" };
	}

	@Override
	public String getInfo() {
		return """
			Transfers an amount of Ł to another user. Transferring Ł can not be undone \
			(unless the other user transfers it back to you).""";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "amount", "user" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "amount of Ł to transfer", "recipient" };
	}

	@Override
	public CommandCategory getCategory() {
		return MONEY;
	}

}
