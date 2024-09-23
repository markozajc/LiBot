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
import static libot.core.FinderUtils.findRoles;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.provider.CustomizationsProvider.Customization;

public class DjRoleCommand extends Command {

	@Nonnull private static final Parameter ROLE_NAME =
		optional(POSITIONAL, "role", "role to use as the DJ role or `disable` to disable");

	public DjRoleCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "djrole")
			.aliases("dj")
			.permissions(false, VOICE_CONNECT, VOICE_SPEAK, MANAGE_SERVER)
			.parameters(ROLE_NAME)
			.description("""
				Manages the DJ role for your guild. DJ role allows you to manage who can use the music commands. \
				If no DJ role is set, everyone will be able to use the music commands. \
				Keep in mind that members with the 'Manage Server' permission (including the guild's owner, of course) \
				can play music regardless of this.

				Run with no parameters to troubleshoot DJ role.
				Run with `disable` as the parameter to unset the DJ role (allow everyone to use music commands)."""));
	}

	@Override
	public void execute(CommandContext c) {
		var provider = c.getGuildCustomization();
		c.arg(ROLE_NAME).map(Argument::value).ifPresentOrElse(roleName -> {
			super.checkPermissions(c);
			switch (roleName) {
				case "disable" -> disable(c, provider);
				default -> set(c, provider, roleName);
			}

		}, () -> {
			report(c, provider);
		});
	}

	private static void set(@Nonnull CommandContext c, @Nonnull Customization cust, @Nonnull String roleName) {
		var roles = findRoles(c, roleName);
		if (roles.isEmpty())
			throw c.errorf("Role \"%s\" does not exist", FAILURE, escape(roleName));

		var role = roles.get(0);
		cust.setDjRole(role);
		c.replyf("DJ role enabled", """
			DJ role set, only members with %s role or the 'Manage Server' permission will be able to \
			manage music from now on.""", SUCCESS, role.getAsMention());
	}

	private static void disable(@Nonnull CommandContext c, @Nonnull Customization cust) {
		cust.setDjRole(null);
		c.replyf("DJ role disabled", "DJ role unset, everyone can manage music from now on.", SUCCESS);
	}

	private void report(@Nonnull CommandContext c, @Nonnull Customization cust) {
		cust.getDjRoleId().ifPresentOrElse(id -> {
			var role = c.getGuild().getRoleById(id);
			if (role != null) {
				c.replyf("""
					The DJ role for this guild is %s.

					Only members with this role or with the 'Manage Server' permission will be able to use music \
					commands.
					Run `%s disable` to disable the DJ role._""", SUCCESS, role.getAsMention(),
						 c.getCommandWithPrefix());

			} else {
				cust.setDjRole(null);
				c.replyf("""
					A DJ role has been configured for this guild, but the role itself has been deleted.

					Everyone has access to the music commands. Set a DJ role with `%s`.
					_Looking for a way to disable DJ role? Run `%s disable` to disable it._""", FAILURE, getUsage(c),
						 c.getCommandWithPrefix());
			}
		}, () -> {
			c.replyf("""
				A DJ role is not configured for this guild.

				Everyone has access to the music commands. Set a DJ role with `%s`.""", DISABLED, getUsage(c),
					 c.getCommandWithPrefix());
		});
	}

}
