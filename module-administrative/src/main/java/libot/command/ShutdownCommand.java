//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
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
package libot.command;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.ADMINISTRATIVE;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.*;

public class ShutdownCommand extends Command {

	@Nonnull private static final Parameter EXIT_CODE = optional(POSITIONAL, "exit code");

	public ShutdownCommand() {
		super(CommandMetadata.builder(ADMINISTRATIVE, "shutdown")
			.description("Shuts down the bot")
			.parameters(EXIT_CODE));
	}

	@Override
	@SuppressFBWarnings(value = "DM_EXIT", justification = "This is the bot's exit point")
	public void execute(CommandContext c) throws Exception {
		int exitCode = c.arg(EXIT_CODE).map(Argument::valueAsInt).orElse(0);
		if (c.confirm("Are you sure you want to shut the bot down?", WARN)) {
			c.react(ACCEPT_EMOJI);
			System.exit(exitCode); // NOSONAR it's required
		}
	}

	@Override
	public void startupCheck(EventContext ec) {
		super.startupCheck(ec);
		ec.requireSysadmin();
	}

}
