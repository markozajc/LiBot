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
import static java.util.concurrent.TimeUnit.*;
import static libot.core.Constants.*;
import static libot.core.command.CommandCategory.MONEY;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntBinaryOperator;

import javax.annotation.Nonnull;

import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.core.ratelimit.Ratelimit;
import libot.provider.MoneyProvider;
import libot.util.*;
import net.dv8tion.jda.api.utils.TimeFormat;

public class RewardCommand extends Command {

	public RewardCommand() {
		super(CommandMetadata.builder(MONEY, "reward")
			.aliases("work")
			.description("Claims your hourly LiBot cash (Ł) reward."));
	}

	private static final long REWARD_SPACING = HOURS.toMillis(1);
	private static final Ratelimit REWARD_RATELIMIT = new Ratelimit(REWARD_SPACING);
	private static final int MIN_REWARD = 5;

	private enum MathOperator {

		PLUS('+', (a, b) -> a + b),
		MINUS('-', (a, b) -> a - b);

		private final char character;
		@Nonnull private final IntBinaryOperator operator;

		MathOperator(char character, @Nonnull IntBinaryOperator operator) {
			this.character = character;
			this.operator = operator;
		}

		public char getCharacter() {
			return this.character;
		}

		public int getResult(int left, int right) {
			return this.operator.applyAsInt(left, right);
		}

	}

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM);

		long time = getRewardRemainingTime(c.getUserIdLong());
		if (time < 0) {
			int left = ThreadLocalRandom.current().nextInt(1, 50);
			int right = ThreadLocalRandom.current().nextInt(1, 50);
			var operator = Utilities.random(MathOperator.values());

			c.setWaiterTimeout(MINUTES, 1);
			var solution = ParseUtils.parseInt(c.askf("Menial labor", "Solve the following:\n> *%s %s %s =*", LITHIUM,
													  left, operator.getCharacter(), right));

			if (solution == operator.getResult(left, right)) {
				var provider = c.getProvider(MoneyProvider.class);
				long earned = computeReward(provider.getBalance(c.getUserIdLong()));
				provider.addMoney(c.getUserIdLong(), earned);
				REWARD_RATELIMIT.register(c.getUserIdLong());
				b.appendDescriptionf("You have earned **%dŁ**\n\n", earned);
				time = REWARD_SPACING;

			} else {
				throw c.errorf("Wrong! Type %s to try again.", FAILURE, monospace(c.getCommandWithPrefix()));
			}
		}

		b.appendDescriptionf("You can claim your next hourly reward %s.",
							 TimeFormat.RELATIVE.format(currentTimeMillis() + time));
		c.reply(b);
	}

	public static long getRewardRemainingTime(long userId) {
		return REWARD_RATELIMIT.check(userId);
	}

	private static long computeReward(long balance) {
		long base = max(round(pow(log(max(1, balance)), 2) * 2), MIN_REWARD);
		return Math.round(base * ThreadLocalRandom.current().nextDouble(0.5D, 2D));
	}

}
