//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.listener;

import static java.lang.System.getenv;
import static java.util.concurrent.TimeUnit.*;
import static libot.command.CalculatorCommand.startProcess;
import static libot.core.Constants.ENV_QALCULATE_EXCHANGE_RATE_UPDATER_PATH;
import static org.eu.zajc.ef.EHandle.handle;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import libot.command.CalculatorCommand;
import libot.core.entity.BotContext;

public class CalculatorRateUpdaterListener implements BotEventListener {

	private static final Logger LOG = getLogger(CalculatorRateUpdaterListener.class);
	private static final long UPDATE_INTERVAL = HOURS.toMillis(1);
	private static final long UPDATE_TIMEOUT = MINUTES.toMillis(5);

	@Override
	public void onStartup(BotContext bot) {
		if (!CalculatorCommand.ENABLED)
			return;

		if (getenv(ENV_QALCULATE_EXCHANGE_RATE_UPDATER_PATH) == null)
			LOG.warn("{} is unset. Exchange rates will not be updated", ENV_QALCULATE_EXCHANGE_RATE_UPDATER_PATH);

		bot.getCron().scheduleAtFixedRate(handle(CalculatorRateUpdaterListener::updateRates, t -> {
			LOG.error("Failed to update exchange rates", t);
		}), 0, UPDATE_INTERVAL, MILLISECONDS);
	}

	private static void updateRates() throws IOException, InterruptedException {
		LOG.debug("Updating exchange rates");

		var p = startProcess(getenv(ENV_QALCULATE_EXCHANGE_RATE_UPDATER_PATH));

		if (!p.waitFor(UPDATE_TIMEOUT, MILLISECONDS)) {
			if (p.isAlive())
				p.destroyForcibly();

			LOG.warn("Updating exchange rates timed out");
		}

		if (p.exitValue() != 0)
			LOG.warn("Updating rates caused non-zero exit {}", p.exitValue());
	}

}
