package libot.core.listeners;

import javax.annotation.Nonnull;

import libot.core.entities.BotContext;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ShredClashListener extends ListenerAdapter {

	private final BotContext bot;

	public ShredClashListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		this.bot.getShredder().resolveClashes(event.getGuild());
	}

}
