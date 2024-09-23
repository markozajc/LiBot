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
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.provider.AutoRoleProvider;
import net.dv8tion.jda.api.entities.Role;

public class AutoRoleCommand extends Command {

	@Nonnull private static final Parameter ROLE_NAME =
		optional(POSITIONAL, "role", "role to use as the auto role or `disable` to disable");

	public AutoRoleCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "autorole")
			.permissions(false, MANAGE_ROLES)
			.parameters(ROLE_NAME)
			.description("""
				Once enabled, every newly joined member will assigned the chosen role.

				Run with no parameters to troubleshoot AutoRole.
				Run with `disable` as the parameter to disable AutoRole."""));
	}

	@Override
	public void execute(CommandContext c) {
		var provider = c.getProvider(AutoRoleProvider.class);
		c.arg(ROLE_NAME).map(Argument::value).ifPresentOrElse(roleName -> {
			super.startupCheck(c);
			switch (roleName) {
				case "disable" -> disable(c, provider);
				default -> set(c, provider, roleName);
			}

		}, () -> {
			report(c, provider);
		});
	}

	private static void report(@Nonnull CommandContext c, @Nonnull AutoRoleProvider provider) {
		String title = "AutoRole health report";
		provider.get(c.getGuildIdLong()).ifPresentOrElse(id -> {
			var self = c.getSelfMember();
			Role role;
			if ((role = c.getGuild().getRoleById(id)) == null) {
				c.reply(title, """
					⚠️ Role does not exist

					AutoRole has been configured, but the role itself has been deleted.

					AutoRole will not function properly.""", FAILURE);

			} else if (!self.hasPermission(MANAGE_ROLES)) {
				c.replyf(title, """
					✅ Role exists
					⚠️ Permission not granted

					AutoRole is configured, but LiBot does not have the 'Manage Roles' permission, which is required.

					AutoRole is set to %s, but will not function properly.""", FAILURE, role.getAsMention());

			} else if (!self.canInteract(role)) {
				c.replyf(title, """
					✅ Role exists
					✅ Permission granted
					⚠️ Hierarchy incorrect

					AutoRole is configured, but LiBot no longer has permission to interact with the set role (%s). \
					Please assign LiBot a role above %s to fix this.

					AutoRole is set to %s, but will not function properly.""", FAILURE, role.getAsMention(),
						 role.getAsMention(), role.getAsMention());

			} else {
				c.replyf(title, """
					✅ Role exists
					✅ Permission granted
					✅ Hierarchy correct

					AutoRole is active and set to %s.
					Run `%s disable` to disable AutoRole.""", SUCCESS, role.getAsMention(), c.getCommandWithPrefix());
			}
		}, () -> c.reply(title, "AutoRole is not enabled.", DISABLED));
	}

	@SuppressWarnings("null")
	private static void set(@Nonnull CommandContext c, @Nonnull AutoRoleProvider provider, @Nonnull String roleName) {
		var roles = findRoles(c, roleName);

		if (roles.isEmpty())
			throw c.errorf("Role \"%s\" does not exist", FAILURE, escape(roleName));

		var role = roles.get(0);

		if (!c.hasGuildPermission(MANAGE_ROLES))
			throw c.error("LiBot must have the 'Manage Roles' permission for this", FAILURE);

		else if (!c.canSelfInteract(role))
			throw c.errorf("%s is higher on the hierarchy than LiBot's is", FAILURE, role.getAsMention());

		else if (!c.canMemberInteract(role))
			throw c.errorf("%s is higher on the hierarchy than yours is", FAILURE, role.getAsMention());

		provider.set(c.getGuildIdLong(), role.getIdLong());
		c.replyf("AutoRole enabled. Every new member will be given %s.", SUCCESS, role.getAsMention());
	}

	private static void disable(@Nonnull CommandContext c, @Nonnull AutoRoleProvider provider) {
		if (provider.get(c.getGuildIdLong()).isEmpty()) {
			throw c.error("AutoRole is not set");

		} else {
			provider.remove(c.getGuildIdLong());
			c.reply("AutoRole disabled. New members will no longer be assigned to a role.", SUCCESS);
		}
	}

}
