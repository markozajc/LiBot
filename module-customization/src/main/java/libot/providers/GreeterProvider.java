package libot.providers;

import static net.dv8tion.jda.api.entities.channel.ChannelType.TEXT;

import javax.annotation.*;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.shred.Shredder;
import libot.providers.GreeterProvider.GreeterConfiguration;
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
		return this.data.getOrDefault(guildId, new GreeterConfiguration());
	}

	public void remove(long guildId) {
		this.data.remove(guildId);
	}

}
