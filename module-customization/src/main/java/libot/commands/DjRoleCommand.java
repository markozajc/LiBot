package libot.commands;

import static libot.core.Constants.*;
import static libot.core.FinderUtils.findRoles;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.CustomizationsProvider.Customization;
import net.dv8tion.jda.api.Permission;

public class DjRoleCommand extends Command {

	private static final String FORMAT_REPORT_DISABLED = """
		A DJ role is not configured for this guild.

		Everyone has access to the music commands. Set a DJ role with `%s`.""";
	private static final String FORMAT_REPORT_DELETED = """
		A DJ role has been configured for this guild, but the role itself has been deleted.

		Everyone has access to the music commands. Set a DJ role with `%s`.
		_Looking for a way to disable DJ role? Run `%s disable` to disable it._""";
	private static final String FORMAT_REPORT_OK = """
		The DJ role for this guild is %s.

		Only members with this role or with the 'Manage Server' permission will be able to use music commands.
		_Looking for a way to disable DJ role? Run `%s disable` to disable it._""";
	private static final String FORMAT_SET_DISABLED = """
		DJ role unset, everyone can manage music from now on.""";
	private static final String FORMAT_SET_OK = """
		DJ role set, only members with %s role or the 'Manage Server' permission will be able to manage music from now \
		on.""";

	@Override
	public void execute(CommandContext c) {
		var customization = c.getGuildCustomization();
		if (c.params().check(0)) {
			super.startupCheck(c);
			set(c, customization);
		} else {
			report(c, customization);
		}
	}

	private static void set(@Nonnull CommandContext c, @Nonnull Customization cust) {
		if ("disable".equalsIgnoreCase(c.params().get(0))) {
			cust.setDjRole(null);
			c.replyf("DJ role disabled", FORMAT_SET_DISABLED, SUCCESS);

		} else {
			var roles = findRoles(c, c.params().get(0));
			if (!roles.isEmpty()) {
				cust.setDjRole(roles.get(0));
				c.replyf("DJ role enabled", FORMAT_SET_OK, SUCCESS, roles.get(0).getAsMention());
			} else {
				c.replyf(FORMAT_ROLE_MISSING, FAILURE, escape(c.params().get(0)));
			}
		}
	}

	private void report(@Nonnull CommandContext c, @Nonnull Customization cust) {
		cust.getDjRoleId().ifPresentOrElse(id -> {
			var role = c.getGuild().getRoleById(id);
			if (role != null) {
				c.replyf(FORMAT_REPORT_OK, SUCCESS, role.getAsMention());
			} else {
				cust.setDjRole(null);
				c.replyf(FORMAT_REPORT_DELETED, FAILURE, getUsage(c), c.getCommandWithPrefix());
			}
		}, () -> c.replyf(FORMAT_REPORT_DISABLED, DISABLED, getUsage(c), c.getCommandWithPrefix()));
	}

	@Override
	public String getName() {
		return "djrole";
	}

	@Override
	public String[] getAliases() {
		return array("dj");
	}

	@Override
	public String getInfo() {
		return """
			Manages the DJ role for your guild. DJ role allows you to manage who can use the music commands. \
			If no DJ role is set, everyone will be able to use the music commands. \
			Keep in mind that members with the 'Manage Server' permission (including the guild's owner, of course) \
			can play music regardless of this.

			Run with no parameters to troubleshoot DJ role.
			Run with `disable` as the parameter to unset the DJ role (allow everyone to use music commands).""";
	}

	@Override
	public Permission[] getPermissions() {
		return array(VOICE_CONNECT, VOICE_SPEAK, MANAGE_SERVER);
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public String[] getParameters() {
		return array("[role]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("role to use as the DJ role or `disable` to allow everyone");
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
