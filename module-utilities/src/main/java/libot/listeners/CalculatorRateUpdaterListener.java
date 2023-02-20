package libot.listeners;

import static com.github.markozajc.ef.EHandle.handle;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.commands.CalculatorCommand;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CalculatorRateUpdaterListener extends ListenerAdapter {

	private static final Logger LOG = getLogger(CalculatorRateUpdaterListener.class);

	private static final long UPDATE_INTERVAL = HOURS.toMillis(1);
	private static final long UPDATE_TIMEOUT = MINUTES.toMillis(5);
	private static final ScheduledExecutorService UPDATER;
	private static boolean updaterRunning;

	static {
		if (CalculatorCommand.ENABLED)
			UPDATER = newSingleThreadScheduledExecutor();
		else
			UPDATER = null;
	}

	@Override
	@SuppressFBWarnings(value = "JLM_JSR166_UTILCONCURRENT_MONITORENTER", justification = "false positive")
	public void onReady(ReadyEvent event) {
		if (UPDATER == null)
			return;

		synchronized (UPDATER) {
			if (updaterRunning)
				return;
			updaterRunning = true;

			UPDATER.scheduleAtFixedRate(handle(CalculatorRateUpdaterListener::updateRates, t -> {
				LOG.error("Failed to update exchange rates", t);
			}), 0, UPDATE_INTERVAL, MILLISECONDS);
		}
	}

	@Override
	@SuppressFBWarnings(value = "JLM_JSR166_UTILCONCURRENT_MONITORENTER", justification = "false positive")
	public void onShutdown(ShutdownEvent event) {
		if (UPDATER == null)
			return;

		synchronized (UPDATER) {
			UPDATER.shutdown();
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
