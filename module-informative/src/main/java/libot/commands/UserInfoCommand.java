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

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.utils.CommandUtils.findMemberOrAuthor;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.TimeFormat;

public class UserInfoCommand extends Command {

	@Nonnull private static final Parameter USER = optional(POSITIONAL, "user");

	public UserInfoCommand() {
		super(CommandMetadata.builder(INFORMATIVE, "userinfo").aliases("user", "ui").parameters(USER).description("""
			Retrieves some of the 'hidden' data about a user. If no one is mentioned, information about you will be \
			displayed."""));
	}

	private static final String FORMAT_PERMISSIONS = "To get %s's permissions for this guild, use %sperms @%s.";
	private static final String FORMAT_TITLE = "Info about %s";

	@Override
	public void execute(CommandContext c) {
		var member = findMemberOrAuthor(c, c.arg(USER));
		var e = new EmbedPrebuilder(LITHIUM);
		e.setThumbnail(member.getEffectiveAvatarUrl());
		e.setTitle(getTitle(member));
		e.addField("ID", codeblock(member.getId()), true);
		e.addField("Account creation date", TimeFormat.DATE_TIME_LONG.format(member.getTimeCreated()), true);
		e.addField("Server join date", TimeFormat.DATE_TIME_LONG.format(member.getTimeJoined()), true);
		e.addField("Roles", getRoles(member), true);
		e.addField("Permissions", getPermissions(c, member));
		c.reply(e);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String getTitle(Member member) {
		var title = new StringBuilder(format(FORMAT_TITLE, escape(member.getUser().getAsTag())));
		if (member.getUser().isBot())
			title.append(" [BOT]");
		if (member.isOwner())
			title.append(" [\uD83D\uDC51]");
		return title.toString();
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String getPermissions(@Nonnull CommandContext c, @Nonnull Member member) {
		String permissions;
		if (member.isOwner())
			permissions = "All of them (server owner bypass)";
		else if (member.hasPermission(ADMINISTRATOR))
			permissions = "All of them (administrator bypass)";
		else
			permissions = format(FORMAT_PERMISSIONS, member.getEffectiveName(), c.getEffectivePrefix(),
								 member.getEffectiveName());
		return codeblock(permissions);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String getRoles(@Nonnull Member member) {
		String roles;
		if (member.getRoles().isEmpty())
			roles = "[none]";
		else
			roles = member.getRoles().stream().map(Role::getName).collect(joining(", @", "@", ""));
		return codeblock(roles);
	}

}
