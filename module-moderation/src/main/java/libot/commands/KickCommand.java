package libot.commands;

import static libot.commands.ModerationCommandUtils.moderationAction;
import static libot.commands.ModerationCommandUtils.ModAction.KICK;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class KickCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		moderationAction(c, KICK);
	}

	@Override
	public String getName() {
		return "kick";
	}

	@Override
	public String getInfo() {
		return "Kicks a member from the guild.";
	}

	@Override
	public Permission[] getPermissions() {
		return new Permission[] { KICK_MEMBERS };
	}

	@Override
	public String[] getParameters() {
		return new String[] { "member", "[reason]" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "name or mention of member to kick", "reason for kick" };
	}

	@Override
	public int getMinParameters() {
		return 1;
	}

	@Override
	public CommandCategory getCategory() {
		return MODERATION;
	}

}
