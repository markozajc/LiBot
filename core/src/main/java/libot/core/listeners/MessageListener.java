package libot.core.listeners;

import static java.util.regex.Pattern.*;

import java.util.*;
import java.util.regex.*;

import javax.annotation.Nonnull;

import libot.core.entities.*;
import libot.providers.CustomizationsProvider;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Nonnull private final BotContext bot;

	public MessageListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	@SuppressWarnings("null")
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!event.isFromGuild() || event.getAuthor().isBot())
			return;

		var prefix = Prefix.resolve(event, this.bot);

		var raw = event.getMessage().getContentRaw();
		if (!prefix.isCommand(raw))
			return;

		var matcher = prefix.getCommandCallMatcher(raw);
		if (matcher.matches())
			throw new IllegalStateException("Message doesn't match the command regex, but isCommand() was true");

		this.bot.getCommands()
			.get(matcher.group(1))
			.ifPresent(c -> c.run(new EventContext(this.bot, event), matcher.group(2)));
	}

	private static record Prefix(@Nonnull String string, long selfId) {

		private static final Map<Prefix, Pattern> PATTERN_CACHE = new HashMap<>();

		@Nonnull
		@SuppressWarnings("null")
		public static Prefix resolve(@Nonnull GenericMessageEvent event, @Nonnull BotContext bot) {
			var prefixString = bot.getProvider(CustomizationsProvider.class)
				.get(event.getGuild().getIdLong())
				.getCustomPrefix()
				.orElse(bot.getConfig().defaultPrefix());

			return new Prefix(prefixString, event.getJDA().getSelfUser().getIdLong());
		}

		public boolean isCommand(@Nonnull String rawContent) {
			return startsWithAndOneMore(rawContent, string())
				|| startsWithAndOneMore(rawContent, "<@!" + selfId() + ">")
				|| startsWithAndOneMore(rawContent, "<@" + selfId() + ">");
		}

		private static boolean startsWithAndOneMore(String input, String prefix) {
			return input.length() > prefix.length() && input.startsWith(prefix);
		}

		@Nonnull
		@SuppressWarnings("null")
		private Matcher getCommandCallMatcher(String input) {
			return PATTERN_CACHE.computeIfAbsent(this, p -> {
				return compile("(?:<@!?%d>|%s) *([^\\s]+)(?:\\s(.*))?".formatted(p.selfId(), quote(p.string())),
							   DOTALL | UNICODE_CHARACTER_CLASS);
			}).matcher(input);

		}

	}

}
