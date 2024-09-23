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
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.ADMINISTRATIVE;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.entity.*;
import libot.provider.ConfigurationProvider;

public class GlobalEnableCommand extends Command {

	private static final MandatoryParameter COMMAND = mandatory(POSITIONAL, "command", "command to enable");

	public GlobalEnableCommand() {
		super(CommandMetadata.builder(ADMINISTRATIVE, "globalenable")
			.aliases("genable")
			.parameters(COMMAND)
			.description("Enables a command globally."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.getCommands().get(c.arg(COMMAND).value()).ifPresentOrElse(cmd -> {
			var conf = c.getProvider(ConfigurationProvider.class);
			if (!conf.isDisabled(cmd)) {
				c.replyf("`%s` is already enabled.", DISABLED, cmd.getName());

			} else {
				conf.enable(cmd);
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
