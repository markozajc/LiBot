package libot.commands.customization;

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.core.FinderUtils.findRoles;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.data.providers.impl.AutoRoleProvider;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

public class AutoRoleCommand extends Command {

	private static final String FORMAT_NOT_SET = """
		AutoRole is not set""";
	private static final String FORMAT_REMOVED = """
		AutoRole disabled. New members will no longer be assigned to a role.""";
	private static final String FORMAT_SET_MISSING_PERMISSION = """
		LiBot must have the 'Manage Roles' permission for this""";
	private static final String FORMAT_SET_WRONG_HIERARCHY = """
		That role is higher in hierarchy list than %s is!""";
	private static final String FORMAT_REPORT_TITLE = "AutoRole health report for %s";
	private static final String FORMAT_SET_OK = """
		AutoRole set. Every member that joins from now on will be assigned to %s.""";
	private static final String FORMAT_REPORT_INACTIVE = """
		AutoRole is not set for. Activate it with `%s`.""";
	private static final String FORMAT_REPORT_TARGET_ROLE_MISSING = """
		⚠ Role does not exist

		AutoRole has been configured, but the role itself has been deleted.

		AutoRole will not work properly. Please fix the configuration with `%s`!""";
	private static final String FORMAT_REPORT_MISSING_PERMISSION = """
		✅ Role exists
		⚠ Permission not granted

		AutoRole is configured, but LiBot does not have the 'Manage Roles' permission, which is required.

		AutoRole is set to %s, but will not function properly.""";
	private static final String FORMAT_REPORT_WRONG_HIERARCHY = """
		✅ Role exists
		✅ Permission granted
		⚠ Hierarchy bad

		AutoRole is configured, but LiBot no longer has permission to interact with the set role (%s). \
		Please assign LiBot a role above %s to fix this.

		AutoRole is set to %s, but will not function properly.""";
	private static final String FORMAT_REPORT_OK = """
		✅ Role exists
		✅ Permission granted
		✅ Hierarchy good

		AutoRole is active and set to %s.""";

	@Override
	public void execute(CommandContext c) {
		var provider = c.provider(AutoRoleProvider.class);
		if (c.params().check(0)) {
			super.startupCheck(c);
			set(c, provider);
		} else {
			report(c, provider);
		}
	}

	private void report(@Nonnull CommandContext c, @Nonnull AutoRoleProvider provider) {
		String title = format(FORMAT_REPORT_TITLE, c.getGuildName());
		provider.get(c.getGuildIdLong()).ifPresentOrElse(id -> {
			var self = c.getSelfMember();
			Role role;
			if ((role = c.getGuild().getRoleById(id)) == null) {
				c.replyf(title, FORMAT_REPORT_TARGET_ROLE_MISSING, FAILURE, getUsage(c));

			} else if (!self.hasPermission(MANAGE_ROLES)) {
				c.replyf(title, FORMAT_REPORT_MISSING_PERMISSION, FAILURE, role.getAsMention());

			} else if (!self.canInteract(role)) {
				c.replyf(title, FORMAT_REPORT_WRONG_HIERARCHY, FAILURE, role.getAsMention(), role.getAsMention(),
						 role.getAsMention());

			} else {
				c.replyf(title, FORMAT_REPORT_OK, SUCCESS, role.getAsMention());
			}
		}, () -> c.replyf(title, FORMAT_REPORT_INACTIVE, DISABLED, getUsage(c)));
	}

	@SuppressWarnings("null")
	private static void set(@Nonnull CommandContext c, @Nonnull AutoRoleProvider provider) {
		var name = c.params().get(0);
		if ("disable".equalsIgnoreCase(name)) {
			if (!provider.get(c.getGuildIdLong()).isEmpty()) {
				c.reply(FORMAT_NOT_SET);

			} else {
				provider.remove(c.getGuildIdLong());
				c.reply(FORMAT_REMOVED, SUCCESS);
			}
		} else {
			var roles = findRoles(c, c.params().get(0));

			if (roles.isEmpty()) {
				c.replyf(FORMAT_ROLE_MISSING, FAILURE, escape(c.params().get(0)));

			} else if (!c.hasGuildPermission(MANAGE_ROLES)) {
				c.reply(FORMAT_SET_MISSING_PERMISSION, FAILURE);

			} else if (!c.canSelfInteract(roles.get(0))) {
				c.replyf(FORMAT_SET_WRONG_HIERARCHY, FAILURE, "LiBot's");

			} else if (!c.canMemberInteract(roles.get(0))) {
				c.replyf(FORMAT_SET_WRONG_HIERARCHY, FAILURE, "yours");

			} else {
				provider.set(c.getGuildIdLong(), roles.get(0).getIdLong());
				c.replyf(FORMAT_SET_OK, SUCCESS, roles.get(0).getAsMention());
			}
		}
	}

	@Override
	public String getName() {
		return "autorole";
	}

	@Override
	public String getInfo() {
		return """
			Once enabled, every newly joined member will assigned the chosen role.

			Run with no parameters to troubleshoot AutoRole.
			Run with `disable` as the parameter to disable AutoRole.""";
	}

	@Override
	public Permission[] getPermissions() {
		return array(MANAGE_ROLES);
	}

	@Override
	public String[] getParameters() {
		return array("[role]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("role to use as the auto role or `disable` to disable");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public void startupCheck(CommandContext c) {
		// Do not perform the permission check - it's performed if the user actually wants to
		// alter the configuration
	}

	@Override
	public CommandCategory getCategory() {
		return CUSTOMIZATION;
	}

}
