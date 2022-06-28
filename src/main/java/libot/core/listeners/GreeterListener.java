package libot.core.listeners;

import javax.annotation.Nonnull;

import libot.core.data.providers.impl.GreeterProvider;
import libot.core.entities.BotContext;
import net.dv8tion.jda.api.entities.*;
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
		var config = this.bot.provider(GreeterProvider.class).get(event.getGuild().getIdLong());
		var channel = event.getGuild().getTextChannelById(config.getChannelId());
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

	@SuppressWarnings("null")
	@Nonnull
	public static String parseMessage(@Nonnull String message, @Nonnull User user, @Nonnull Guild guild) {
		return message.replace("{name}", user.getName())
			.replace("{discrim}", user.getDiscriminator())
			.replace("{ping}", user.getAsMention())
			.replace("{guild}", guild.getName());
	}

}
