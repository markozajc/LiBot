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
package libot.util;

import static java.util.concurrent.TimeUnit.SECONDS;
import static libot.core.Constants.*;

import javax.annotation.Nonnull;

import libot.core.listener.EventWaiterListener;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.*;

public class EventUtils {

	private int timeout = 0;
	@Nonnull private final EventWaiterListener ewl;
	@Nonnull private final User user;
	@Nonnull private final MessageChannelUnion channel;

	public EventUtils(@Nonnull EventWaiterListener ewl, @Nonnull User user, @Nonnull MessageChannelUnion channel) {
		this(ewl, user, channel, 0);
	}

	public EventUtils(@Nonnull EventWaiterListener ewl, @Nonnull User user, @Nonnull MessageChannelUnion channel,
					  int timeout) {
		this.user = user;
		this.channel = channel;
		this.ewl = ewl;
		this.timeout = timeout;
	}

	@Nonnull
	public MessageReaction getReaction(@Nonnull Message message) throws InterruptedException {
		return this.ewl.awaitEvent(p -> {
			GenericMessageReactionEvent e = (GenericMessageReactionEvent) p;
			return e.getUserIdLong() == this.user.getIdLong() && e.getMessageIdLong() == message.getIdLong();
		}, p -> {
			try {
				message.getChannel().retrieveMessageById(message.getIdLong()).complete();
				return false;
			} catch (RuntimeException e) {
				return true;
			}
		}, this.timeout, SECONDS, GenericMessageReactionEvent.class).getReaction();
	}

	@Nonnull
	@SuppressWarnings("null")
	public Message getMessage(@Nonnull Emoji emoji) throws InterruptedException {
		GenericMessageReactionEvent event = this.ewl.awaitEvent(p -> {

			GenericMessageReactionEvent e = (GenericMessageReactionEvent) p;

			return e.getUserIdLong() == this.user.getIdLong() && e.getChannel().getIdLong() == this.channel.getIdLong()
				&& e.getReaction().getEmoji().equals(emoji);

		}, null, this.timeout, SECONDS, GenericMessageReactionEvent.class);

		return event.getChannel().retrieveMessageById(event.getMessageIdLong()).complete();
	}

	@Nonnull
	public Message awaitMessage(boolean ignoreBlank) throws InterruptedException {
		return this.ewl.awaitEvent(p -> {

			var e = (MessageReceivedEvent) p;

			return e.getAuthor().getIdLong() == this.user.getIdLong()
				&& e.getChannel().getIdLong() == this.channel.getIdLong()
				&& (!ignoreBlank || !e.getMessage().getContentRaw().isBlank());

		}, null, this.timeout, SECONDS, MessageReceivedEvent.class).getMessage();
	}

	public boolean awaitBoolean(@Nonnull Message question, boolean keepPrompt) throws InterruptedException {
		var event = this.ewl.awaitEvent(p -> {
			MessageReactionAddEvent e = (MessageReactionAddEvent) p;
			var emoji = e.getReaction().getEmoji();

			return e.getUserIdLong() == this.user.getIdLong() && e.getMessageIdLong() == question.getIdLong()
				&& (ACCEPT_EMOJI.equals(emoji) || DECLINE_EMOJI.equals(emoji));

		}, p -> {
			try {
				question.getChannel().retrieveMessageById(question.getIdLong()).complete();
				return false;
			} catch (RuntimeException e) {
				return true;
			}

		}, this.timeout, SECONDS, MessageReactionAddEvent.class);

		boolean result = ACCEPT_EMOJI.equals(event.getReaction().getEmoji());

		if (!keepPrompt)
			question.delete().queue();
		return result;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
