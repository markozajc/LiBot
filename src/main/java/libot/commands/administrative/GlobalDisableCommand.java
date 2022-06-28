package libot.commands.administrative;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.data.providers.impl.ConfigurationProvider;
import libot.core.entities.CommandContext;

public class GlobalDisableCommand extends Command {

	private static final String FORMAT_NOT_FOUND = "`%s` does not exist.";
	private static final String FORMAT_ALREADY_DISABLED = "`%s` is already disabled.";

	@Override
	public void execute(CommandContext c) {
		var cmd = c.getCommands().get(c.params().get(0));
		var conf = c.provider(ConfigurationProvider.class);
		if (cmd == null) {
			c.replyf(FORMAT_NOT_FOUND, FAILURE, c.params().get(0));

		} else if (conf.isDisabled(cmd)) {
			c.replyf(FORMAT_ALREADY_DISABLED, DISABLED, cmd.getName());

		} else if (cmd instanceof GlobalEnableCommand) {
			c.react(DENY_EMOJI);

		} else {
			conf.disable(cmd);
			c.react(ACCEPT_EMOJI);
		}
	}

	@Override
	public String getName() {
		return "globaldisablecommand";
	}

	@Override
	public String[] getAliases() {
		return array("gdisable");
	}

	@Override
	public String getInfo() {
		return "Disables a command globally.";
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
