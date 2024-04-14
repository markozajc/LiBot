package libot.commands;

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.utils.CommandUtils.findMemberOrAuthor;

import java.util.EnumSet;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class PermissionsCommand extends Command {

	private static final String FORMAT_OWNER_NOTICE = """
		%s %s. They have all permissions and bypass channel overrides.""";
	private static final String FORMAT_TITLE = "%s's permissions";

	@SuppressWarnings("null")
	@Override
	public void execute(CommandContext c) {
		var member = findMemberOrAuthor(c);
		var listed = EnumSet.noneOf(Permission.class);
		var e = new EmbedPrebuilder(LITHIUM);
		member.getRoles()
			.stream()
			.map(r -> forPermissions(r.getPermissions(), r.getName(), listed))
			.forEach(e::addField);
		e.addField(forPermissions(c.getPublicRole().getPermissions(), "@everyone", listed));
		e.setTitlef(FORMAT_TITLE, member.getEffectiveName());

		if (member.isOwner())
			e.setFooterf(FORMAT_OWNER_NOTICE, member.getEffectiveName(), "is the owner");
		else if (member.hasPermission(Permission.ADMINISTRATOR))
			e.setFooterf(FORMAT_OWNER_NOTICE, member.getEffectiveName(), "has the 'Administrator' permission");

		c.reply(e);
	}

	@Nonnull
	private static Field forPermissions(@Nonnull EnumSet<Permission> perms, @Nonnull String roleName,
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

	@Override
	public String getName() {
		return "permissions";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "perms" };
	}

	@Override
	public String getInfo() {
		return """
			Lists someone's permissions and roles they inherited them from. This command will NOT list any \
			channel-specific permission overrides. If no member is mentioned, your permissions will be listed.""";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "[user]" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "user to get permissions of" };
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return INFORMATIVE;
	}
}
