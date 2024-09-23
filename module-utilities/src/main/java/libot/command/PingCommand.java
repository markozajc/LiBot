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

import static libot.core.command.CommandCategory.UTILITIES;

import libot.core.command.*;
import libot.core.entity.CommandContext;

public class PingCommand extends Command {

	public PingCommand() {
		super(CommandMetadata.builder(UTILITIES, "ping")
			.description("Responds with _Pong!_ and bot's current ping in milliseconds."));
	}

	@Override
	public void execute(CommandContext c) throws InterruptedException {
		long wsPing = c.getJda().getGatewayPing();
		c.getJda().getRestPing().queue(restPing -> {
			c.replyf("""
				Pong!
				HTTP ping: **%s** ms,
				WebSocket ping: **%s** ms.""", restPing, wsPing);
		});
	}

}
