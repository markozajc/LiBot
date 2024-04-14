package libot.commands;

import static java.lang.String.format;
import static libot.core.Constants.FAILURE;
import static libot.core.commands.CommandCategory.UTILITIES;

import java.util.concurrent.ThreadLocalRandom;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class DiceCommand extends Command {

	private static final int DEFAULT_SIDES = 6;
	private static final char BASE_EMOJI = 9855;
	private static final String GENERIC_EMOJI = "\uD83C\uDFB2";

	@Override
	public void execute(CommandContext c) {
		int sides = c.params().getIntOrDefault(0, DEFAULT_SIDES);

		if (sides < 1)
			throw c.error("The number of sides must be larger than 1", FAILURE);

		int rolled = ThreadLocalRandom.current().nextInt(1, sides + 1);
		var emoji = rolled > 6 ? GENERIC_EMOJI : Character.toString(BASE_EMOJI + rolled);
		c.replyf("%s The dice has rolled on number **%d**.", emoji, rolled);
	}

	@Override
	public String getName() {
		return "dice";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "roll" };
	}

	@Override
	public String getInfo() {
		return "Rolls a dice.";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "[sides]" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { format("number of sides, %d by default", DEFAULT_SIDES) };
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
