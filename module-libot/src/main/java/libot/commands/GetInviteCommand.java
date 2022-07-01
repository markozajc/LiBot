package libot.commands;

import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.LIBOT;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class GetInviteCommand extends Command {

	private static final String FORMAT_INVITE = """
		To invite LiBot to your guild, click [here](https://libot.eu.org/get) and follow further instructions!""";

	@Override
	public void execute(CommandContext c) throws Exception {
		c.reply(FORMAT_INVITE, LITHIUM);
	}

	@Override
	public String getName() {
		return "getinvite";
	}

	@Override
	public String[] getAliases() {
		return array("add", "invite", "getlibot");
	}

	@Override
	public String getInfo() {
		return """
			Displays LiBot's invitation link.""";
	}

	@Override
	public CommandCategory getCategory() {
		return LIBOT;
	}

}
