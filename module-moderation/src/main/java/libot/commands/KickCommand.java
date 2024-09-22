package libot.commands;

import static libot.commands.ModerationCommandUtils.*;
import static libot.commands.ModerationCommandUtils.ModAction.KICK;
import static libot.core.commands.CommandCategory.MODERATION;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class KickCommand extends Command {

	public KickCommand() {
		super(CommandMetadata.builder(MODERATION, "kick")
			.permissions(KICK_MEMBERS)
			.parameters(MEMBER, REASON)
			.description("Kicks a member from the guild and notifies them with a private message."));
	}

	@Override
	public void execute(CommandContext c) {
		moderationAction(c, KICK);
	}

}
