package libot.commands;

import static libot.commands.ModerationCommandUtils.*;
import static libot.core.Constants.ACCEPT_EMOJI;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class AddRoleCommand extends Command {

	public AddRoleCommand() {
		super(CommandMetadata.builder(MODERATION, "addrole")
			.permissions(MANAGE_ROLES)
			.parameters(MEMBER, ROLE)
			.description("Adds a role to a member."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var tuple = getRoleMemberTuple(c);
		c.getGuild().addRoleToMember(tuple.target(), tuple.role()).queue();
		c.react(ACCEPT_EMOJI);
	}

}
