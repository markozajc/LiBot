package libot.commands;

import static libot.commands.ModerationCommandUtils.moderationAction;
import static libot.commands.ModerationCommandUtils.ModAction.BAN;
import static libot.core.commands.CommandCategory.MODERATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class BanCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		moderationAction(c, BAN);
	}

	@Override
	public String getName() {
		return "ban";
	}

	@Override
	public String getInfo() {
		return "Bans a member from the guild.";
	}

	@Override
	public Permission[] getPermissions() {
		return array(BAN_MEMBERS);
	}

	@Override
	public String[] getParameters() {
		return array("member", "[reason]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("name or mention of member to ban", "reason for ban");
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
