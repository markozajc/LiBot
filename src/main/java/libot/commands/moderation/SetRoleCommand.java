package libot.commands.moderation;

import static libot.commands.moderation.ModerationCommandUtils.getRoleMemberTuple;
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.commands.CommandCategory.MODERATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class SetRoleCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().modifyMemberRoles(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

	@Override
	public String getName() {
		return "setrole";
	}

	@Override
	public String getInfo() {
		return "Sets a role for the a member (removes all other roles and adds the chosen one).";
	}

	@Override
	public Permission[] getPermissions() {
		return array(MANAGE_ROLES);
	}

	@Override
	public String[] getParameters() {
		return array("member", "role");
	}

	@Override
	public CommandCategory getCategory() {
		return MODERATION;
	}

}
