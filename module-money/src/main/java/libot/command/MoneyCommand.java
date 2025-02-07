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
package libot.command;

import static java.lang.System.currentTimeMillis;
import static libot.core.Constants.LITHIUM;
import static libot.core.command.CommandCategory.MONEY;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;

import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.provider.MoneyProvider;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MoneyCommand extends Command {

	public MoneyCommand() {
		super(CommandMetadata.builder(MONEY, "money")
			.aliases("balance")
			.description("Displays your current LiBot cash (Ł) balance."));
	}

	@Override
	public void execute(CommandContext c) {
		var provider = c.getProvider(MoneyProvider.class);
		long balance = provider.getBalance(c.getUserIdLong());

		var b = new EmbedPrebuilder(LITHIUM);
		b.setDescriptionf("Your current balance is: **%dŁ**\n\n", balance);

		long rewardRemainingTime = RewardCommand.getRewardRemainingTime(c.getUserIdLong());
		if (rewardRemainingTime < 0) {
			b.appendDescriptionf("You can claim your hourly reward with %s.",
								 monospace(c.getCommandWithPrefix(RewardCommand.class)));

		} else {
			b.appendDescriptionf("You can claim your next hourly reward %s.",
								 TimeFormat.RELATIVE.format(currentTimeMillis() + rewardRemainingTime));
		}
		c.reply(b);
	}

}
