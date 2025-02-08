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
package libot.command;

import static libot.core.Constants.LITHIUM;
import static libot.core.command.CommandCategory.LIBOT;

import libot.core.command.*;
import libot.core.entity.CommandContext;

public class GetInviteCommand extends Command {

	public GetInviteCommand() {
		super(CommandMetadata.builder(LIBOT, "getinvite")
			.aliases("add", "invite", "getlibot")
			.description("Displays LiBot's invite link."));
	}

	@Override
	public void execute(CommandContext c) throws Exception {
		c.reply("To invite LiBot to your guild, click [here](https://libot.eu.org/get) and follow the instructions!",
				LITHIUM);
	}

}
