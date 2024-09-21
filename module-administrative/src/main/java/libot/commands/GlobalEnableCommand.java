package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.entities.*;
import libot.providers.ConfigurationProvider;

public class GlobalEnableCommand extends Command {

	private static final MandatoryParameter COMMAND = mandatory(POSITIONAL, "command", "The command to enable");

	public GlobalEnableCommand() {
		super(CommandMetadata.builder(ADMINISTRATIVE, "globalenable")
			.aliases("genable")
			.parameters(COMMAND)
			.description("Enables a command globally."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.getCommands().get(c.arg(COMMAND).value()).ifPresentOrElse(cmd -> {
			var conf = c.getProvider(ConfigurationProvider.class);
			if (!conf.isDisabled(cmd)) {
				c.replyf("`%s` is already enabled.", DISABLED, cmd.getName());

			} else {
				conf.enable(cmd);
				c.react(ACCEPT_EMOJI);
			}

		}, () -> {
			c.replyf("`%s` does not exist.", FAILURE, c.arg(COMMAND).value());
		});
	}

	@Override
	public void startupCheck(EventContext ec) {
		super.startupCheck(ec);
		ec.requireSysadmin();
	}

}
