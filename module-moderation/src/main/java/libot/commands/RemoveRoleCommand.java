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
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class RemoveRoleCommand extends Command {

	public RemoveRoleCommand() {
		super(CommandMetadata.builder(MODERATION, "removerole")
			.aliases("rmrole")
			.permissions(MANAGE_ROLES)
			.parameters(MEMBER, ROLE)
			.description("Removes a role from a member."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().removeRoleFromMember(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

}
