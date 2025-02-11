//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.command;

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.GAMES;
import static libot.util.Utilities.getDigits;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.command.exception.runtime.NumberOverflowException;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.util.ParseUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class GuessANumberCommand extends Command {

	@Nonnull private static final Parameter MAX_NUMBER =
		optional(POSITIONAL, "max number", "upper bound for the hidden number, 100 by default");

	public GuessANumberCommand() {
		super(CommandMetadata.builder(GAMES, "guessanumber")
			.aliases("guessnumber", "guessanum", "guessnum")
			.parameters(MAX_NUMBER)
			.description("Plays a game of \"Guess My Number\"."));
	}

	private static final String FOOTER = "Type in your guess or EXIT to exit";
	private static final String FORMAT_OUT_OF_BOUNDS = """
		Try guessing numbers **%s**!""";

	private static final int LOWEST_BOUND = 2;
	private static final int MAX_HISTORY = (MessageEmbed.VALUE_MAX_LENGTH + 1) / 22;
	// 22 = "`..........` - higher\n".length(), the theoretical maximum, minus 1 for the
	// last newline

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		int bound = c.arg(MAX_NUMBER).map(Argument::valueAsInt).orElse(100);
		if (bound == Integer.MAX_VALUE)
			throw new NumberOverflowException(); // since bound is +1, which would overflow

		if (bound < LOWEST_BOUND)
			throw c.errorf("The max number parameter must not be lower than **%d**.", FAILURE, LOWEST_BOUND);

		int answer = ThreadLocalRandom.current().nextInt(1, bound + 1);

		c.reply("Guess a number between 0 and %d".formatted(bound), FOOTER, LITHIUM);

		var history = IntLists.mutable.withInitialCapacity(MAX_HISTORY + 1);
		int count = 0; // can't just use history since it's capped to MAX_HISTORY
		boolean won = false;
		do {
			var input = c.ask().toLowerCase();
			if (input.endsWith("exit")) {
				if (c.confirm("Are you sure you want to quit the game?", WARN))
					throw c.cancel();

			} else {
				count++;
				won = guess(c, input, history, answer, bound, count);
			}
		} while (!won);
	}

	private static boolean guess(@Nonnull CommandContext c, @Nonnull String input, @Nonnull MutableIntList history,
								 int answer, int bound, int count) {
		try {
			int guess = ParseUtils.parseInt(input);
			if (guess == answer) {
				c.replyf("That is correct! My number is %d, and you've guessed it in **%d tries**.", SUCCESS, answer,
						 count);
				return true;

			} else if (checkRange(c, bound, guess)) {
				history.add(guess);
				report(c, history, guess, answer, bound, count);
			}

		} catch (NumberFormatException e) {
			c.replyf("Please provide a number (or EXIT to exit).", FAILURE);

		} catch (NumberOverflowException e) {
			c.replyf(FORMAT_OUT_OF_BOUNDS, FAILURE, "below or equal to " + bound);
		}
		return false;
	}

	@SuppressWarnings("null")
	private static void report(@Nonnull CommandContext c, @Nonnull MutableIntList history, int guess, int number,
							   int bound, int count) {
		if (history.size() > MAX_HISTORY)
			history.removeAtIndex(0);

		var e = new EmbedPrebuilder(LITHIUM);
		e.setFooter(FOOTER);
		e.setDescriptionf("My number is **%s** than your guess!", guess > number ? "lower" : "higher");
		var guesses = history.primitiveStream().mapToObj(g -> {
			return "`%s` - %s".formatted(rightPad(Integer.toString(g), getDigits(bound)),
										 g > number ? "lower" : "higher");
		}).collect(joining("\n"));
		e.addField("Your guesses (%d)".formatted(count), guesses);

		c.reply(e);
	}

	private static boolean checkRange(@Nonnull CommandContext c, int bound, int guess) {
		String limitClue = null;
		if (guess > bound)
			limitClue = "below or equal to " + bound;
		else if (guess <= 0)
			limitClue = "above 0";

		if (limitClue != null)
			c.replyf(FORMAT_OUT_OF_BOUNDS, FAILURE, limitClue);
		return limitClue == null;
	}

}
