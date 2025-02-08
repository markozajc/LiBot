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

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.command.CommandCategory.INFORMATIVE;

import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;

public class GetRolesCommand extends Command {

	public GetRolesCommand() {
		super(CommandMetadata.builder(INFORMATIVE, "getroles")
			.aliases("roles")
			.description("Lists all roles in the guild."));
	}

	@Override
	public void execute(CommandContext c) {
		c.getGuild().loadMembers().onSuccess(members -> {
			var b = new EmbedPrebuilder(LITHIUM);
			b.setDescription(c.getGuild().getRoles().stream().map(r -> {
				var count = members.stream().filter(m -> m.getRoles().contains(r)).count();
				return "- %s (%d member%s)".formatted(r.getAsMention(), count, count == 1 ? "" : "s");
			}).collect(joining("\n")));
			c.reply(b);
		});
	}

}
