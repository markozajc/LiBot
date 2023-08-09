package libot.listeners;

import static java.util.concurrent.TimeUnit.*;
import static org.eu.zajc.ef.EHandle.handle;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import libot.commands.CalculatorCommand;
import libot.core.entities.BotContext;

public class CalculatorRateUpdaterListener implements BotEventListener {

	private static final Logger LOG = getLogger(CalculatorRateUpdaterListener.class);
	private static final long UPDATE_INTERVAL = HOURS.toMillis(1);
	private static final long UPDATE_TIMEOUT = MINUTES.toMillis(5);

	@Override
	public void onStartup(BotContext bot) {
		if (CalculatorCommand.ENABLED) {
			bot.cron().scheduleAtFixedRate(handle(CalculatorRateUpdaterListener::updateRates, t -> {
				LOG.error("Failed to update exchange rates", t);
			}), 0, UPDATE_INTERVAL, MILLISECONDS);
		}
	}

	private static void updateRates() throws IOException, InterruptedException {
		LOG.debug("Updating exchange rates");

		var p = CalculatorCommand.executeQalculate("update");

		if (!p.waitFor(UPDATE_TIMEOUT, MILLISECONDS)) {
			if (p.isAlive())
				p.destroyForcibly();

			LOG.warn("Updating exchange rates timed out");
		}

		if (p.exitValue() != 0)
			LOG.warn("Updating rates caused non-zero exit {}", p.exitValue());
	}

}
