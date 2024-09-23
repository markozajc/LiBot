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
package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.entities.*;
import libot.providers.ConfigurationProvider;

public class GlobalDisableCommand extends Command {

	private static final MandatoryParameter COMMAND = mandatory(POSITIONAL, "command", "command to disable");

	public GlobalDisableCommand() {
		super(CommandMetadata.builder(ADMINISTRATIVE, "globaldisable")
			.aliases("gdisable")
			.parameters(COMMAND)
			.description("Disables a command globally."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.getCommands().get(c.arg(COMMAND).value()).ifPresentOrElse(cmd -> {
			var conf = c.getProvider(ConfigurationProvider.class);
			if (conf.isDisabled(cmd)) {
				c.replyf("`%s` is already disabled.", DISABLED, cmd.getName());

			} else if (cmd instanceof GlobalEnableCommand) {
				c.react(DENY_EMOJI);

			} else {
				conf.disable(cmd);
				c.react(ACCEPT_EMOJI);
			}

		}, () -> {
			c.replyf("`%s` does not exist.", FAILURE, c.arg(COMMAND).value());
		});
	}

	@Override
	public void startupCheck(EventContext ec) {
		super.startupCheck(ec);
		ec.requireSysadmin();
	}

}
