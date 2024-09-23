package libot.commands;

import static libot.core.Constants.FAILURE;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.UTILITIES;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class DiceCommand extends Command {

	private static final int DEFAULT_SIDES = 6;

	@SuppressWarnings("null") @Nonnull private static final Parameter SIDES =
		optional(POSITIONAL, "sides", "number of sides, %d by default".formatted(DEFAULT_SIDES));

	public DiceCommand() {
		super(CommandMetadata.builder(UTILITIES, "dice").aliases("roll", "die").parameters(SIDES).description("""
			Rolls a dice."""));
	}

	private static final char BASE_EMOJI = '\u267F';
	private static final String GENERIC_EMOJI = "\uD83C\uDFB2";

	@Override
	public void execute(CommandContext c) {
		int sides = c.arg(SIDES).map(Argument::valueAsInt).orElse(DEFAULT_SIDES);

		if (sides < 1)
			throw c.error("The number of sides must be larger than 1", FAILURE);

		int rolled = ThreadLocalRandom.current().nextInt(1, sides + 1);
		var emoji = rolled > 6 ? GENERIC_EMOJI : Character.toString(BASE_EMOJI + rolled);
		c.replyf("%s The dice has rolled on number **%d**.", emoji, rolled);
	}

}
