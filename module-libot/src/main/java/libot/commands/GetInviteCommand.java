package libot.commands;

import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.LIBOT;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class GetInviteCommand extends Command {

	public GetInviteCommand() {
		super(CommandMetadata.builder(LIBOT, "getinvite")
			.aliases("add", "invite", "getlibot")
			.description("Displays LiBot's invite link.")
			.build());
	}

	@Override
	public void execute(CommandContext c) throws Exception {
		c.reply("To invite LiBot to your guild, click [here](https://libot.eu.org/get) and follow the instructions!",
				LITHIUM);
	}

}
