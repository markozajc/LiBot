package libot.commands;

import static libot.commands.ModerationCommandUtils.*;
import static libot.commands.ModerationCommandUtils.ModAction.BAN;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class BanCommand extends Command {

	public BanCommand() {
		super(CommandMetadata.builder(MODERATION, "ban")
			.permissions(BAN_MEMBERS)
			.parameters(MEMBER, REASON)
			.description("Bans a member from the guild and notifies them with a private message."));
	}

	@Override
	public void execute(CommandContext c) {
		moderationAction(c, BAN);
	}

}
