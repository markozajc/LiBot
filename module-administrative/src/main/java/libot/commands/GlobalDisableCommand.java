package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.entities.*;
import libot.providers.ConfigurationProvider;

public class GlobalDisableCommand extends Command {

	private static final MandatoryParameter COMMAND = mandatory(POSITIONAL, "command", "The command to disable");
	@Nonnull private static final CommandMetadata META =
		new CommandMetadata.Builder(ADMINISTRATIVE, "globaldisable").description("Disables a command globally.")
			.aliases("gdisable")
			.parameters(COMMAND)
			.build();

	public GlobalDisableCommand() {
		super(META);
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.getCommands().get(c.arg(COMMAND).value()).ifPresentOrElse(cmd -> {
			var conf = c.getProvider(ConfigurationProvider.class);
			if (conf.isDisabled(cmd)) {
				c.replyf("`%s` is already disabled.", DISABLED, cmd.getName());

			} else if (cmd instanceof GlobalEnableCommand) {
				c.react(DENY_EMOJI);

			} else {
				conf.disable(cmd);
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
