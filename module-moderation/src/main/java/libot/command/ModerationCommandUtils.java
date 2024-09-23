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

import static java.util.concurrent.TimeUnit.DAYS;
import static libot.core.Constants.*;
import static libot.core.FinderUtils.*;
import static libot.core.argument.ParameterList.Parameter.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static net.dv8tion.jda.api.Permission.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.dv8tion.jda.api.utils.MarkdownUtil.bold;

import java.util.function.Function;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.*;
import libot.core.entity.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

final class ModerationCommandUtils {

	private static final String ERROR_MEMBER_HIERARCHY =
		"You can not interact with members in higher/equal roles than you";
	private static final String FORMAT_DOES_NOT_EXIST = "%s \"%s\" does not exist";

	static final MandatoryParameter MEMBER = mandatory(POSITIONAL, "member");
	static final MandatoryParameter ROLE = mandatory(POSITIONAL, "role");
	static final Parameter REASON = optional(POSITIONAL, "reason");

	static record RoleMemberTuple(Role role, Member target) {}

	@Nonnull
	static RoleMemberTuple getRoleMemberTuple(@Nonnull CommandContext c) {
		var member = findMember(c);
		if (!c.canMemberInteract(member))
			throw c.error(ERROR_MEMBER_HIERARCHY, FAILURE);

		var role = findRole(c);
		if (!c.canMemberInteract(role))
			throw c.error("You can not set roles higher/equal than your highest role", FAILURE);

		return new RoleMemberTuple(role, member);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Member findMember(@Nonnull CommandContext c) {
		var members = findMembers(c, c.arg(MEMBER));
		if (members.isEmpty())
			throw c.errorf(FORMAT_DOES_NOT_EXIST, FAILURE, "Member", escape(c.arg(MEMBER).value()));
		return members.get(0);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Role findRole(@Nonnull CommandContext c) {
		var roles = findRoles(c, c.arg(ROLE));
		if (roles.isEmpty())
			throw c.errorf(FORMAT_DOES_NOT_EXIST, FAILURE, "Role", escape(c.arg(ROLE).value()));
		return roles.get(0);
	}

	public enum ModAction {

		KICK(Member::kick, KICK_MEMBERS, "kicked out of"),
		BAN(t -> t.ban(0, DAYS), BAN_MEMBERS, "banned from");

		@Nonnull private final Function<Member, AuditableRestAction<Void>> action;
		@Nonnull private final Permission permission;
		@Nonnull private final String verb;

		ModAction(@Nonnull Function<Member, AuditableRestAction<Void>> action, @Nonnull Permission permission,
				  @Nonnull String verb) {
			this.action = action;
			this.permission = permission;
			this.verb = verb;
		}

		@Nonnull
		public RestAction<Void> act(@Nonnull Member target, @Nonnull String reason) {
			return this.action.apply(target).reason(reason);
		}

		@Nonnull
		public Permission getPermission() {
			return this.permission;
		}

		@Nonnull
		public String getVerb() {
			return this.verb;
		}

	}

	@SuppressWarnings("null")
	static void moderationAction(@Nonnull CommandContext c, @Nonnull ModAction action) {
		var target = findMember(c);

		if (target.equals(c.getSelfMember()))
			throw c.errorf("I can not %s myself", FAILURE, action.name().toLowerCase());

		if (!c.canMemberInteract(target))
			throw c.error(ERROR_MEMBER_HIERARCHY, FAILURE);

		c.ensureGuildPermission(action.getPermission());
		c.ensureSelfInteract(target);

		var confirm = c.confirmf("Are you sure you want to %s **%s**?", action.name().toLowerCase(),
								 target.getUser().getAsMention());
		if (!confirm)
			throw c.cancel();

		var reason = c.arg(REASON);

		Runnable act = () -> action
			.act(target, "%s: %s".formatted(c.getUserTag(), reason.map(Argument::value).orElse("No reason specified.")))
			.queue();

		if (!target.getUser().isBot()) {
			var message = new StringBuilder("You've been ");
			message.append(action.getVerb());
			message.append(' ');
			message.append(bold(escape(c.getGuildName())));

			reason.map(Argument::value).ifPresentOrElse(r -> {
				message.append(" for the following reason:\n");
				message.append(escape(r));
			}, () -> {
				message.append(" (no reason was specified)");
			});

			target.getUser()
				.openPrivateChannel()
				.flatMap(p -> p.sendMessage(message))
				.queue(m -> act.run(), e -> act.run());

		} else {
			act.run();
		}

		c.react(ACCEPT_EMOJI);
	}

	private ModerationCommandUtils() {}

}
