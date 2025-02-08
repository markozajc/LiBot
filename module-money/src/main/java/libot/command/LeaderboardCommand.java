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

import static libot.core.command.CommandCategory.MONEY;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.text.*;
import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;

import libot.core.Constants;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.provider.MoneyProvider;

public class LeaderboardCommand extends Command {

	private static final DecimalFormat FORMAT;
	static {
		var dfs = new DecimalFormatSymbols();
		dfs.setGroupingSeparator(',');
		FORMAT = new DecimalFormat("#", dfs);
		FORMAT.setGroupingUsed(true);
		FORMAT.setGroupingSize(3);
	}

	public LeaderboardCommand() {
		super(CommandMetadata.builder(MONEY, "leaderboard")
			.description("Displays the LiBot cash (Ł) balances of guild members."));
	}

	@Override
	public void execute(CommandContext c) {
		var provider = c.getProvider(MoneyProvider.class);
		c.getGuild().findMembers(m -> provider.hasBalance(m.getIdLong())).onSuccess(members -> {
			var leaderboard = members.stream()
				.map(m -> Pair.of(provider.getBalance(m.getIdLong()), m.getAsMention()))
				.sorted(Comparator.<Pair<Long, String>, Long>comparing(Pair::getLeft).reversed())
				.map(p -> Pair.of(FORMAT.format((long) p.getLeft()), p.getRight()))
				.toList();

			var b = new EmbedPrebuilder(Constants.LITHIUM);
			b.setTitlef("Leaderboard for %s", c.getGuildName());

			if (leaderboard.isEmpty()) {
				b.setDescription("*No member has any LiBot cash*");

			} else {
				int balanceLength = leaderboard.stream().map(Pair::getLeft).mapToInt(String::length).max().getAsInt();

				for (var entry : leaderboard)
					b.appendDescriptionf("**`%s Ł`** %s\n", leftPad(entry.getLeft(), balanceLength), entry.getRight());
			}
			c.reply(b);
		});
	}

}
