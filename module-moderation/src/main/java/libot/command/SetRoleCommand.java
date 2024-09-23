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

import static libot.command.ModerationCommandUtils.*;
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.command.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.command.*;
import libot.core.entity.CommandContext;

public class SetRoleCommand extends Command {

	public SetRoleCommand() {
		super(CommandMetadata.builder(MODERATION, "setrole")
			.permissions(MANAGE_ROLES)
			.parameters(MEMBER, ROLE)
			.description("Sets a role for the member (removes all other roles and adds the chosen one)."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().modifyMemberRoles(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

}
