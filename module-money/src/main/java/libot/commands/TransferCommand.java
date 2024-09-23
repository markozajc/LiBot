//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.commands;

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.core.FinderUtils.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.MONEY;
import static libot.providers.MoneyProvider.DEFAULT_BALANCE;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.MoneyProvider;
import net.dv8tion.jda.api.entities.User;

public class TransferCommand extends Command {

	@Nonnull private static final MandatoryParameter BALANCE = mandatory(POSITIONAL, "balance", "balance to transfer");
	@Nonnull private static final MandatoryParameter USER = mandatory(POSITIONAL, "user", "recipient");

	public TransferCommand() {
		super(CommandMetadata.builder(MONEY, "transfer")
			.aliases("sendmoney", "donate")
			.parameters(BALANCE, USER)
			.description("""
				Transfers Ł balance to another user. Transferring Ł can not be undone unless the recipient transfers \
				it back to you."""));
	}

	@Override
	public void execute(CommandContext c) {
		var recipient = getRecipient(c);
		var provider = c.getProvider(MoneyProvider.class);
		int amount = getBalance(c, provider);

		if (c.confirmf("Are you sure you want to transfer **%dŁ** to %s?", LITHIUM, amount,
					   escape(recipient.getAsMention()))) {

			provider.takeMoney(c.getUserIdLong(), amount);
			provider.addMoney(recipient.getIdLong(), amount);

			c.replyf("Transfer successful", "%dŁ has been transferred to %s's wallet", SUCCESS, amount,
					 escape(recipient.getAsMention()));
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static User getRecipient(@Nonnull CommandContext c) {
		var targets = preferFromGuild(c, findUsers(c, c.arg(USER).value()));
		if (targets.isEmpty())
			throw c.errorf("User \"%s\" was not found", FAILURE, escape(c.arg(USER).value()));

		var recipient = targets.get(0);

		if (c.getUser().equals(recipient))
			throw c.error("You can not transfer Ł to yourself", DISABLED);

		if (recipient.isBot())
			throw c.error("You can not transfer Ł to a bot", DISABLED);

		return recipient;
	}

	private static int getBalance(@Nonnull CommandContext c, @Nonnull MoneyProvider provider) {
		int amount = c.arg(BALANCE).valueAsInt();
		if (amount < 1)
			throw c.error("The amount must be positive", FAILURE);

		if (amount > provider.getBalance(c.getUserIdLong())) {
			throw c.errorf("Insufficient funds! Check your balance with `%s`", FAILURE,
						   c.getCommandWithPrefix(MoneyCommand.class));
		}

		long maxTransfer = provider.getBalance(c.getUserIdLong()) - DEFAULT_BALANCE;
		if (amount > maxTransfer) {
			throw c.errorf("You must not have less than **%dŁ** after the transfer; you %s.", FAILURE, DEFAULT_BALANCE,
						   maxTransfer <= 0 ? "can't transfer any Ł"
							   : format("can transfer at most **%dŁ**", maxTransfer));
		}

		return amount;
	}

}
