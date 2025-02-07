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
package libot.provider;

import static java.lang.Long.compare;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import javax.annotation.*;

import org.slf4j.*;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.provider.TimedTaskProvider;
import libot.core.entity.CommandContext;
import libot.core.shred.Shredder;
import libot.provider.TimerProvider.UserTimer;
import net.dv8tion.jda.api.entities.Message;

public class TimerProvider extends TimedTaskProvider<UserTimer> {

	private static final Logger LOG = LoggerFactory.getLogger(TimerProvider.class);

	public static record UserTimer(long userId, @Nullable String text, long endTime, long guildId, long channelId,
		long messageId) implements TimedTaskProvider.TimedTask {

		public UserTimer(@Nullable String text, long endTime, @Nonnull CommandContext c) {
			this(c.getUserIdLong(), text, endTime, c.getGuildIdLong(), c.getChannelIdLong(), c.getMessageIdLong());
		}

	}

	public TimerProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "timers", "timer");
	}

	@Override
	@SuppressWarnings("null")
	public void onExpiry(UserTimer timer) {
		var jumpLink = Message.JUMP_URL.formatted(timer.guildId(), timer.channelId(), timer.messageId());
		var message = "**\u23F3 Timer:** [%s](%s)".formatted(requireNonNullElse(timer.text(), "Beep, beep!"), jumpLink);
		getShredder().sendPrivateMessage(timer.userId(), message).exceptionally(t -> {
			LOG.warn("Failed to deliver a timer message to {}", timer.userId());
			LOG.warn("", t);
			return null;
		});
	}

	@Nonnull
	@SuppressWarnings("null")
	public List<UserTimer> getTimers(long userId) {
		return this.data.stream()
			.filter(t -> t.userId() == userId)
			.sorted((t1, t2) -> compare(t1.endTime(), t2.endTime()))
			.toList();
	}

}
