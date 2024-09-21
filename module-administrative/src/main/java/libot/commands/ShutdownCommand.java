package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.*;

public class ShutdownCommand extends Command {

	@Nonnull private static final Parameter EXIT_CODE = Parameter.optional(POSITIONAL, "exit code");
	@Nonnull private static final CommandMetadata META =
		new CommandMetadata.Builder(ADMINISTRATIVE, "killprocess").description("Shuts down the bot")
			.parameters(EXIT_CODE)
			.build();

	public ShutdownCommand() {
		super(META);
	}

	@Override
	@SuppressFBWarnings(value = "DM_EXIT", justification = "This is the bot's exit point")
	public void execute(CommandContext c) throws Exception {
		if (c.confirm("Are you sure you want to shut the bot down?", WARN)) {
			c.react(ACCEPT_EMOJI);
			System.exit(c.arg(EXIT_CODE).map(Argument::valueAsInt).orElse(0)); // NOSONAR it's required
		}
	}

	@Override
	public void startupCheck(EventContext ec) {
		super.startupCheck(ec);
		ec.requireSysadmin();
	}

}
