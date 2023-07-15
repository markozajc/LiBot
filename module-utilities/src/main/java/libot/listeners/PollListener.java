package libot.listeners;

import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import libot.core.entities.BotContext;
import libot.core.shred.Shredder.Shred;
import libot.providers.PollProvider;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;

public class PollListener extends ListenerAdapter {

	private static final Logger LOG = getLogger(PollListener.class);

	@Nonnull private final BotContext bot;

	public PollListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if (isSelf(event) || !hasPermission(event))
			return;

		this.bot.provider(PollProvider.class)
			.getData()
			.stream()
			.filter(p -> p.getMessageId() == event.getMessageIdLong())
			.filter(p -> !p.allowsMultipleVotes())
			.findAny()
			.ifPresent(p -> event.retrieveMessage()
				.submit()
				.thenApply(Message::getReactions)
				.thenAccept(rs -> rs.stream()
					.filter(r -> !r.getReactionEmote().getName().equals(event.getReactionEmote().getName()))
					.map(r -> r.removeReaction(event.getUser()))
					.forEach(RestAction::queue))
				.exceptionally(t -> {
					LOG.debug("Could not remove a reaction", t);
					return null;
				}));
	}

	private static boolean hasPermission(@Nonnull GuildMessageReactionAddEvent event) {
		return event.getGuild().getSelfMember().hasPermission(event.getChannel(), MESSAGE_MANAGE);
	}

	private boolean isSelf(@Nonnull GuildMessageReactionAddEvent event) {
		return this.bot.shredder().getShreds().stream().mapToLong(Shred::id).anyMatch(i -> i == event.getUserIdLong());
	}

}
