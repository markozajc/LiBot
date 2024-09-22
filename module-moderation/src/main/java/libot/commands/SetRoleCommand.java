package libot.commands;

import static libot.commands.ModerationCommandUtils.*;
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class SetRoleCommand extends Command {

	public SetRoleCommand() {
		super(CommandMetadata.builder(MODERATION, "setrole")
			.permissions(MANAGE_ROLES)
			.parameters(MEMBER, ROLE)
			.description("Sets a role for the member (removes all other roles and adds the chosen one)."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().modifyMemberRoles(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

}
