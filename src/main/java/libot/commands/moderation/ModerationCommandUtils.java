package libot.commands.moderation;

import static java.lang.String.format;
import static libot.core.Constants.FAILURE;
import static libot.core.FinderUtils.*;
import static net.dv8tion.jda.api.Permission.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import java.util.function.BiFunction;

import javax.annotation.Nonnull;

import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;

final class ModerationCommandUtils {

	private static final String FORMAT_DIRECT_REASON = """
		You got %s of **%s** for the following reason:
		%s""";
	private static final String FORMAT_DIRECT_NO_REASON = """
		You got %s out of **%s** (no reason was specified).""";
	private static final String FORMAT_DOES_NOT_EXIST = """
		%s "%s" does not exist""";
	private static final String FORMAT_MEMBER_HIERARCHY_ERROR = """
		You can not interact with members in higher/equal roles than you""";
	private static final String FORMAT_ROLE_HIERARCHY_ERROR = """
		You can not set roles higher/equal than your highest role""";

	static record RoleMemberTuple(@Nonnull Role role, @Nonnull Member target) {}

	@Nonnull
	static RoleMemberTuple getRoleMemberTuple(@Nonnull CommandContext c) {
		var member = findMember(c);
		if (!c.canMemberInteract(member))
			throw c.error(FORMAT_MEMBER_HIERARCHY_ERROR, FAILURE);
		var role = findRole(c);
		if (!c.canMemberInteract(role))
			throw c.error(FORMAT_ROLE_HIERARCHY_ERROR, FAILURE);
		return new RoleMemberTuple(role, member);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Member findMember(@Nonnull CommandContext c) {
		var members = findMembers(c, c.params().get(0));
		if (members.isEmpty())
			throw c.errorf(FORMAT_DOES_NOT_EXIST, FAILURE, "Member", escape(c.params().get(0)));
		return members.get(0);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Role findRole(@Nonnull CommandContext c) {
		var roles = findRoles(c, c.params().get(1));
		if (roles.isEmpty())
			throw c.errorf(FORMAT_DOES_NOT_EXIST, FAILURE, "Role", escape(c.params().get(1)));
		return roles.get(0);
	}

	public enum ModAction {

		KICK(Member::kick, KICK_MEMBERS, "kicked out of"),
		BAN((t, r) -> t.ban(0, r), BAN_MEMBERS, "banned from");

		@Nonnull
		private final BiFunction<Member, String, RestAction<Void>> action;
		@Nonnull
		private final Permission permission;
		@Nonnull
		private final String verb;

		ModAction(@Nonnull BiFunction<Member, String, RestAction<Void>> action, @Nonnull Permission permission,
				  @Nonnull String verb) {
			this.action = action;
			this.permission = permission;
			this.verb = verb;
		}

		@Nonnull
		@SuppressWarnings("null")
		public RestAction<Void> act(@Nonnull Member target, @Nonnull String reason) {
			return this.action.apply(target, reason);
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
			throw c.error(FORMAT_MEMBER_HIERARCHY_ERROR, FAILURE);

		c.ensureGuildPermission(action.getPermission());
		c.ensureSelfInteract(target);

		var confirm = c.confirmf("Are you sure you want to %s **%s**?", action.name().toLowerCase(),
								 target.getUser().getAsMention());
		if (!confirm)
			throw c.cancel();

		var reason = c.params().getOrDefault(1, null);

		Runnable act = () -> action
			.act(target, "%s: %s".formatted(c.getUserTag(), reason == null ? "No reason specified." : reason))
			.queue();

		if (!target.getUser().isBot()) {
			String message;
			if (reason != null)
				message = format(FORMAT_DIRECT_REASON, action.getVerb(), escape(c.getGuildName(), true), reason);
			else
				message = format(FORMAT_DIRECT_NO_REASON, action.getVerb(), escape(c.getGuildName(), true));

			target.getUser()
				.openPrivateChannel()
				.flatMap(p -> p.sendMessage(message))
				.queue(m -> act.run(), e -> act.run());
		} else {
			act.run();
		}

	}

	private ModerationCommandUtils() {}

}
