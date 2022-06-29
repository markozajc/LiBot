package libot.commands;

import static libot.core.commands.CommandCategory.UTILITIES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class PingCommand extends Command {

	private static final String FORMAT_PONG = """
		Pong!
		HTTP ping: **%s** ms,
		WebSocket ping: **%s** ms.""";

	@Override
	public void execute(CommandContext c) throws InterruptedException {
		long wsPing = c.jda().getGatewayPing();
		c.jda().getRestPing().queue(restPing -> c.replyf(FORMAT_PONG, restPing, wsPing));
	}

	@Override
	public String getInfo() {
		return "Responds with _Pong!_ and bot's current ping in milliseconds.";
	}

	@Override
	public String getName() {
		return "ping";
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
