//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
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

import javax.annotation.Nonnull;

import org.eclipse.collections.api.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;

public class DeletionRequestListener extends ListenerAdapter {

	public static final String DELETION_REACTION = "\uD83D\uDDD1\uFE0F";
	public static final MutableLongLongMap MESSAGE_INITIATOR_CACHE = LongLongMaps.mutable.empty();

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getMessageAuthorIdLong() != event.getJDA().getSelfUser().getIdLong()
			|| !(event.getReaction().getEmoji() instanceof UnicodeEmoji r) || !r.getName().equals(DELETION_REACTION))

			return;

		if (event.getChannel() instanceof PrivateChannel) {
			deleteMessage(event).queue();
			return;
		}

		var expectedId = MESSAGE_INITIATOR_CACHE.getIfAbsent(event.getMessageIdLong(), -1);
		if (expectedId == event.getUserIdLong()) {
			deleteMessage(event).queue();

		} else if (expectedId == -1) {
			event.retrieveMessage()
				.map(Message::getMessageReference)
				.flatMap(MessageReference::resolve)
				.map(Message::getAuthor)
				.flatMap(initiator -> {
					if (initiator.getIdLong() == event.getUserIdLong()) {
						return deleteMessage(event);

					} else {
						MESSAGE_INITIATOR_CACHE.put(event.getMessageIdLong(), initiator.getIdLong());
						return denyRequest(event);
					}
				})
				.queue();

		} else {
			denyRequest(event).queue();
		}
	}

	@Nonnull
	private static RestAction<Object> deleteMessage(MessageReactionAddEvent event) {
		return event.getChannel().deleteMessageById(event.getMessageIdLong()).map(e -> null);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static RestAction<Object> denyRequest(@Nonnull MessageReactionAddEvent event) {
		return event.getChannel()
			.sendMessage("<@%s>, you may only delete messages that respond to you.".formatted(event.getUserId()))
			.map(e -> null);
	}

}
