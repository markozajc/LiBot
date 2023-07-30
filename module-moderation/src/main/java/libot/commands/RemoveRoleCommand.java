package libot.commands;

import static libot.commands.ModerationCommandUtils.getRoleMemberTuple;
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.commands.CommandCategory.MODERATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class RemoveRoleCommand extends Command {

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().addRoleToMember(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

	@Override
	public String getName() {
		return "removerole";
	}

	@Override
	public String[] getAliases() {
		return array("rmrole");
	}

	@Override
	public String getInfo() {
		return "Removes a role from a member.";
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
