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
package libot.listeners;

import javax.annotation.Nonnull;

import libot.core.entities.BotContext;
import libot.providers.GreeterProvider;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GreeterListener extends ListenerAdapter {

	private final BotContext bot;

	public GreeterListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		run(event);
	}

	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		run(event);
	}

	private void run(@Nonnull GenericGuildEvent event) {
		var config = this.bot.getProvider(GreeterProvider.class).get(event.getGuild().getIdLong());
		var channel = (MessageChannelUnion) event.getGuild()
			.getChannelCache()
			.getElementById(config.getChannelType(), config.getChannelId());

		if (channel == null || !channel.canTalk())
			return;

		String message;
		User user;
		if (event instanceof GuildMemberJoinEvent gmje) {
			message = config.getWelcomeMessage();
			user = gmje.getUser();

		} else if (event instanceof GuildMemberRemoveEvent gmre) {
			message = config.getGoodbyeMessage();
			user = gmre.getUser();

		} else {
			return;
		}

		if (message != null)
			channel.sendMessage(parseMessage(message, user, event.getGuild())).queue();
	}

	@Nonnull
	@SuppressWarnings("null")
	public static String parseMessage(@Nonnull String message, @Nonnull User user, @Nonnull Guild guild) {
		return message.replace("{name}", user.getName())
			.replace("{discrim}", user.getDiscriminator())
			.replace("{ping}", user.getAsMention())
			.replace("{guild}", guild.getName());
	}

}
