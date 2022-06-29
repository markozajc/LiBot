package libot.providers;

import javax.annotation.*;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.shred.Shredder;
import libot.providers.GreeterProvider.GreeterConfiguration;

public class GreeterProvider extends SnowflakeProvider<GreeterConfiguration> {

	public static class GreeterConfiguration {

		@Nullable
		private String welcomeMessage;
		@Nullable
		private String goodbyeMessage;
		private long channelId;

		public long getChannelId() {
			return this.channelId;
		}

		public void setChannel(long channelId) {
			this.channelId = channelId;
		}

		@Nullable
		public String getWelcomeMessage() {
			return this.welcomeMessage;
		}

		@Nullable
		public String getGoodbyeMessage() {
			return this.goodbyeMessage;
		}

		public void setWelcomeMessage(@Nullable String welcomeMessage) {
			this.welcomeMessage = welcomeMessage;
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
		return this.data.getOrDefault(guildId, new GreeterConfiguration());
	}

	public void remove(long guildId) {
		this.data.remove(guildId);
	}

}
