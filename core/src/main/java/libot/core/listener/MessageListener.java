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
package libot.core.listener;

import static java.util.regex.Pattern.*;

import java.util.*;
import java.util.regex.*;

import javax.annotation.Nonnull;

import libot.core.entity.*;
import libot.provider.CustomizationsProvider;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Nonnull private final BotContext bot;

	public MessageListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	@SuppressWarnings("null")
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild() || event.getAuthor().isBot())
			return;

		var prefix = Prefix.resolve(event, this.bot);

		var raw = event.getMessage().getContentRaw();
		if (!prefix.isCommand(raw))
			return;

		var matcher = prefix.getCommandCallMatcher(raw);
		if (!matcher.matches())
			throw new IllegalStateException("Message doesn't match the command regex, but isCommand() was true");

		this.bot.getCommands()
			.get(matcher.group(1))
			.ifPresent(c -> c.run(new EventContext(this.bot, event), matcher.group(2)));
	}

	private static record Prefix(@Nonnull String string, long selfId) {

		private static final Map<Prefix, Pattern> PATTERN_CACHE = new HashMap<>();

		@Nonnull
		@SuppressWarnings("null")
		public static Prefix resolve(@Nonnull GenericMessageEvent event, @Nonnull BotContext bot) {
			var prefixString = bot.getProvider(CustomizationsProvider.class)
				.get(event.getGuild().getIdLong())
				.getCustomPrefix()
				.orElse(bot.getConfig().defaultPrefix());

			return new Prefix(prefixString, event.getJDA().getSelfUser().getIdLong());
		}

		public boolean isCommand(@Nonnull String rawContent) {
			return startsWithAndOneMore(rawContent, string())
				|| startsWithAndOneMore(rawContent, "<@!" + selfId() + ">")
				|| startsWithAndOneMore(rawContent, "<@" + selfId() + ">");
		}

		private static boolean startsWithAndOneMore(String input, String prefix) {
			return input.length() > prefix.length() && input.startsWith(prefix);
		}

		@Nonnull
		@SuppressWarnings("null")
		private Matcher getCommandCallMatcher(String input) {
			return PATTERN_CACHE.computeIfAbsent(this, p -> {
				return compile("(?:<@!?%d>|%s) *([^\\s]+)(?:\\s(.*))?".formatted(p.selfId(), quote(p.string())),
							   DOTALL | UNICODE_CHARACTER_CLASS);
			}).matcher(input);

		}

	}

}
