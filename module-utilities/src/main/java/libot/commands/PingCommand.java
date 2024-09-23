package libot.commands;

import static libot.core.commands.CommandCategory.UTILITIES;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class PingCommand extends Command {

	public PingCommand() {
		super(CommandMetadata.builder(UTILITIES, "ping")
			.description("Responds with _Pong!_ and bot's current ping in milliseconds."));
	}

	@Override
	public void execute(CommandContext c) throws InterruptedException {
		long wsPing = c.getJda().getGatewayPing();
		c.getJda().getRestPing().queue(restPing -> {
			c.replyf("""
				Pong!
				HTTP ping: **%s** ms,
				WebSocket ping: **%s** ms.""", restPing, wsPing);
		});
	}

}
