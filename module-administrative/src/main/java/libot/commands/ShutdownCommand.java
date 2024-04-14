package libot.commands;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class ShutdownCommand extends Command {

	@Override
	@SuppressFBWarnings(value = "DM_EXIT", justification = "This is the bot's exit point")
	public void execute(CommandContext c) throws Exception {
		if (c.confirm("Are you sure you want to shut the bot down?", WARN)) {
			c.react(ACCEPT_EMOJI);
			System.exit(c.params().getIntOrDefault(0, 0)); // NOSONAR it's required
		}
	}

	@Override
	public String getName() {
		return "shutdown";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "[exit code]" };
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public String getInfo() {
		return """
			Shuts the bot down""";
	}

	@Override
	public void startupCheck(CommandContext c) {
		super.startupCheck(c);
		c.requireSysadmin();
	}

	@Override
	public CommandCategory getCategory() {
		return ADMINISTRATIVE;
	}

}
