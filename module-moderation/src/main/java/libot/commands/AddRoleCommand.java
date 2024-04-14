package libot.commands;

import static libot.commands.ModerationCommandUtils.getRoleMemberTuple;
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class AddRoleCommand extends Command {

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().addRoleToMember(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

	@Override
	public String getName() {
		return "addrole";
	}

	@Override
	public String getInfo() {
		return "Adds a role to a member.";
	}

	@Override
	public Permission[] getPermissions() {
		return new Permission[] { MANAGE_ROLES };
	}

	@Override
	public String[] getParameters() {
		return new String[] { "member", "role" };
	}

	@Override
	public CommandCategory getCategory() {
		return MODERATION;
	}

}
