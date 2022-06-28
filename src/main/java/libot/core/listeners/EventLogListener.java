package libot.core.listeners;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import libot.core.entities.BotContext;
import libot.core.shred.Shredder.Shred;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventLogListener extends ListenerAdapter {

	private static final Logger LOG = getLogger("EventLog");

	private final BotContext bot;

	public EventLogListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onReady(ReadyEvent event) {
		this.bot.shredder()
			.getShreds()
			.stream()
			.filter(s -> s.id() == event.getJDA().getSelfUser().getIdLong())
			.findAny()
			.map(Shred::name)
			.ifPresentOrElse(n -> LOG.info("{} is online", n), () -> {
				throw new IllegalStateException("Unknown bot readied (id: %d)"
					.formatted(event.getJDA().getSelfUser().getIdLong()));
			});
	}

	@Override
	public void onException(ExceptionEvent event) {
		if (!event.isLogged())
			LOG.error("Caught an exception", event.getCause());
	}

}
