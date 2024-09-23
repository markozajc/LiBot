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

import static net.dv8tion.jda.api.entities.channel.ChannelType.TEXT;

import javax.annotation.*;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.provider.SnowflakeProvider;
import libot.core.shred.Shredder;
import libot.provider.GreeterProvider.GreeterConfiguration;
import net.dv8tion.jda.api.entities.channel.ChannelType;

public class GreeterProvider extends SnowflakeProvider<GreeterConfiguration> {

	public static class GreeterConfiguration {

		@Nullable private String welcomeMessage;
		@Nullable private String goodbyeMessage;
		@Nonnull private ChannelType channelType = TEXT;
		private long channelId;

		public long getChannelId() {
			return this.channelId;
		}

		@Nonnull
		public ChannelType getChannelType() {
			return this.channelType;
		}

		public void setChannel(long channelId, @Nonnull ChannelType type) {
			this.channelId = channelId;
			this.channelType = type;
		}

		@Nullable
		public String getWelcomeMessage() {
			return this.welcomeMessage;
		}

		public void setWelcomeMessage(@Nullable String welcomeMessage) {
			this.welcomeMessage = welcomeMessage;
		}

		@Nullable
		public String getGoodbyeMessage() {
			return this.goodbyeMessage;
		}

		public void setGoodbyeMessage(@Nullable String goodbyeMessage) {
			this.goodbyeMessage = goodbyeMessage;
		}

	}

	public GreeterProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "greet");
	}

	@Nonnull
	@SuppressWarnings("null")
	public GreeterConfiguration get(long guildId) {
		return this.data.computeIfAbsent(guildId, id -> new GreeterConfiguration());
	}

	public void remove(long guildId) {
		this.data.remove(guildId);
	}

}
