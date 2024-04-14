package libot.commands;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.utils.CommandUtils.findMemberOrAuthor;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.TimeFormat;

public class UserInfoCommand extends Command {

	private static final String FORMAT_PERMISSIONS = "To get %s's permissions for this guild, use %sperms @%s.";
	private static final String FORMAT_TITLE = "Info about %s";
	private static final String FORMAT_CODEBLOCK = """
		```
		%s```""";

	@Override
	public void execute(CommandContext c) {
		var member = findMemberOrAuthor(c);
		var e = new EmbedPrebuilder(LITHIUM);
		e.setThumbnail(member.getEffectiveAvatarUrl());
		e.setTitle(getTitle(member));
		e.addFieldf("ID", FORMAT_CODEBLOCK, member.getId(), true);
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
			roles = member.getRoles().stream().map(Role::getAsMention).collect(joining(", "));
		return codeblock(roles);
	}

	@Override
	public String getName() {
		return "userinfo";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "user", "ui" };
	}

	@Override
	public String getInfo() {
		return """
			Retrieves some of the 'hidden' data about a user. If no one is mentioned, information about you will be \
			displayed.""";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "[user]" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "user to get information about" };
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
