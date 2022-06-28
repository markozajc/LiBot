package libot.commands.administrative;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.data.providers.impl.ConfigurationProvider;
import libot.core.entities.CommandContext;

public class GlobalEnableCommand extends Command {

	private static final String FORMAT_NOT_FOUND = "`%s` does not exist.";
	private static final String FORMAT_ALREADY_ENABLED = "%s is already enabled.";

	@Override
	public void execute(CommandContext c) {
		var cmd = c.getCommands().get(c.params().get(0));
		var conf = c.provider(ConfigurationProvider.class);
		if (cmd == null) {
			c.replyf(FORMAT_NOT_FOUND, FAILURE, c.params().get(0));

		} else if (!conf.isDisabled(cmd)) {
			c.replyf(FORMAT_ALREADY_ENABLED, DISABLED, cmd.getName());

		} else {
			conf.enable(cmd);
			c.react(ACCEPT_EMOJI);
		}
	}

	@Override
	public String getName() {
		return "globalenablecommand";
	}

	@Override
	public String[] getAliases() {
		return array("genable");
	}

	@Override
	public String getInfo() {
		return "Enables a command globally.";
	}

	@Override
	public String[] getParameters() {
		return array("command");
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
