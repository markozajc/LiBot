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

import static libot.commands.ModerationCommandUtils.*;
import static libot.commands.ModerationCommandUtils.ModAction.BAN;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class BanCommand extends Command {

	public BanCommand() {
		super(CommandMetadata.builder(MODERATION, "ban")
			.permissions(BAN_MEMBERS)
			.parameters(MEMBER, REASON)
			.description("Bans a member from the guild and notifies them with a private message."));
	}

	@Override
	public void execute(CommandContext c) {
		moderationAction(c, BAN);
	}

}
