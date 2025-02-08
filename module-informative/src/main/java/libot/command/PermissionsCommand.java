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

import static com.google.common.collect.Lists.reverse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static libot.core.Constants.LITHIUM;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.INFORMATIVE;
import static libot.util.CommandUtils.findMemberOrAuthor;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.Role;

public class PermissionsCommand extends Command {

	@Nonnull private static final Parameter USER = optional(POSITIONAL, "user", "user to get permissions of");

	public PermissionsCommand() {
		super(CommandMetadata.builder(INFORMATIVE, "permissions").aliases("perms").parameters(USER).description("""
			Lists someone's permissions and roles they inherited them from. This command will NOT list any \
			channel-specific permission overrides. If no member is mentioned, your permissions will be listed."""));
	}

	private static final String FORMAT_OWNER_NOTICE = """
		%s %s. They have all permissions and bypass channel overrides.""";
	private static final String FORMAT_TITLE = "%s's permissions";

	@SuppressWarnings("null")
	@Override
	public void execute(CommandContext c) {
		var member = findMemberOrAuthor(c, c.arg(USER));
		var listed = EnumSet.noneOf(Permission.class);
		var e = new EmbedPrebuilder(LITHIUM);

		var roleFields = concat(member.getRoles().stream(), Stream.of(c.getPublicRole()))
			.sorted(Comparator.comparingInt(Role::getPosition))
			.map(r -> forRole(r.getPermissions(), r.getName(), listed))
			.toList();
		reverse(roleFields).forEach(e::addField);

		e.setTitlef(FORMAT_TITLE, member.getEffectiveName());

		if (member.isOwner())
			e.setFooterf(FORMAT_OWNER_NOTICE, member.getEffectiveName(), "is the owner");
		else if (member.hasPermission(Permission.ADMINISTRATOR))
			e.setFooterf(FORMAT_OWNER_NOTICE, member.getEffectiveName(), "has the 'Administrator' permission");

		c.reply(e);
	}

	@Nonnull
	private static Field forRole(@Nonnull EnumSet<Permission> perms, @Nonnull String roleName,
								 @Nonnull EnumSet<Permission> listed) {
		var b = new StringBuilder();
		var permissions = perms.clone();
		permissions.removeAll(listed);

		if (permissions.isEmpty()) {
			b.append("_All already inherited_");

		} else {
			b.append(permissions.stream().map(Permission::getName).collect(joining(", ")));
			listed.addAll(permissions);
		}

		return new Field(roleName, b.toString(), false);
	}

}
