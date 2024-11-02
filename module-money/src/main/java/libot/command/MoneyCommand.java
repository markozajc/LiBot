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

import static java.lang.Math.*;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.HOURS;
import static libot.core.Constants.LITHIUM;
import static libot.core.command.CommandCategory.MONEY;

import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.core.ratelimit.Ratelimit;
import libot.provider.MoneyProvider;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MoneyCommand extends Command {

	public MoneyCommand() {
		super(CommandMetadata.builder(MONEY, "money")
			.aliases("balance", "work", "reward")
			.description("Displays your current balance and claims your hourly reward."));
	}

	private static final long REWARD_SPACING = HOURS.toMillis(1);
	private static final Ratelimit REWARD_RATELIMIT = new Ratelimit(REWARD_SPACING);
	private static final int MIN_REWARD = 5;

	@Override
	public void execute(CommandContext c) {
		var provider = c.getProvider(MoneyProvider.class);
		long balance = provider.getBalance(c.getUserIdLong());

		var b = new EmbedPrebuilder(LITHIUM);
		b.setDescriptionf("Your current balance is: **%dŁ**", balance);

		long time = REWARD_RATELIMIT.check(c.getUserIdLong());
		if (time == -1) {
			long earned = computeReward(balance);
			provider.addMoney(c.getUserIdLong(), earned);
			REWARD_RATELIMIT.register(c.getUserIdLong());
			b.appendDescriptionf(" + %d (hourly reward)", earned);
			time = REWARD_SPACING;
		}

		b.appendDescriptionf("\nYou can claim your next hourly reward %s.",
							 TimeFormat.RELATIVE.format(currentTimeMillis() + time));
		c.reply(b);
	}

	private static long computeReward(long balance) {
		return max(round(pow(log(max(1, balance)), 2) * 2), MIN_REWARD);
	}

}
