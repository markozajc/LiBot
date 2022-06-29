package libot.listeners;

import javax.annotation.Nonnull;

import libot.core.entities.BotContext;
import libot.providers.AutoRoleProvider;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoRoleListener extends ListenerAdapter {

	@Nonnull
	private final BotContext bot;

	public AutoRoleListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (event.getUser().isBot())
			return;

		this.bot.provider(AutoRoleProvider.class).get(event.getGuild().getIdLong()).ifPresent(roleid -> {
			var role = event.getGuild().getRoleById(roleid);
			if (role == null)
				return;

			event.getGuild().addRoleToMember(event.getMember(), role).reason("Autorole").queue();
		});
	}
}
