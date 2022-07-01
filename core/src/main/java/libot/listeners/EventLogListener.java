package libot.listeners;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventLogListener extends ListenerAdapter {

	private static final Logger LOG = getLogger("EventLog");

	@Override
	public void onException(ExceptionEvent event) {
		if (!event.isLogged())
			LOG.error("Caught an exception", event.getCause());
	}

}
