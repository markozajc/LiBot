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
package libot.listener;

import javax.annotation.Nonnull;

import libot.core.entity.BotContext;
import libot.provider.AutoRoleProvider;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoRoleListener extends ListenerAdapter {

	@Nonnull private final BotContext bot;

	public AutoRoleListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (event.getUser().isBot())
			return;

		this.bot.getProvider(AutoRoleProvider.class).get(event.getGuild().getIdLong()).ifPresent(roleid -> {
			var role = event.getGuild().getRoleById(roleid);
			if (role == null)
				return;

			event.getGuild().addRoleToMember(event.getMember(), role).reason("Autorole").queue();
		});
	}
}
