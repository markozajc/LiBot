package libot.commands;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.utils.Utilities.*;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;

import libot.core.commands.*;
import libot.core.commands.exceptions.runtime.NumberOverflowException;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.utils.ParseUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class GuessANumberCommand extends Command {

	private static final String FORMAT_TIP = "Type in your guess or EXIT to exit";
	private static final String FORMAT_LOW_BOUND = """
		The max number parameter must not be lower than **%d**.""";
	private static final String FORMAT_STATUS = """
		My number is **%s** than your guess!""";
	private static final String FORMAT_CORRECT = """
		That is correct! My number is %d, and you guessed it in **%d tries**.""";
	private static final String FORMAT_OUT_OF_BOUND = """
		Try guessing numbers **%s**!""";
	private static final String FORMAT_CONFIRM_EXIT = """
		Are you sure you want to quit the game?""";
	private static final String FORMAT_NAN = """
		Please provide a number (or EXIT to exit).""";

	private static final int LOWEST_BOUND = 2;
	private static final int MAX_HISTORY = (MessageEmbed.VALUE_MAX_LENGTH + 1) / 22;
	// 22 = "`..........` - higher\n".length(), the theoretical maximum, minus 1 for the
	// last newline

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		int bound = c.params().getIntOrDefault(0, 100);
		if (bound == Integer.MAX_VALUE)
			throw new NumberOverflowException(); // since bound is +1, which goes around

		if (bound < LOWEST_BOUND)
			throw c.errorf(FORMAT_LOW_BOUND, FAILURE, LOWEST_BOUND);

		int answer = ThreadLocalRandom.current().nextInt(1, bound + 1);

		c.reply("Guess a number between 0 and %d".formatted(bound), FORMAT_TIP, LITHIUM);

		var history = IntLists.mutable.withInitialCapacity(MAX_HISTORY + 1);
		int count = 0; // can't just use history since it's capped to MAX_HISTORY
		boolean won = false;
		do {
			var resp = c.askraw();
			var input = resp.getContentDisplay();
			if ("exit".equals(input)) {
				if (c.confirm(FORMAT_CONFIRM_EXIT, WARN)) {
					resp.addReaction(ACCEPT_EMOJI).queue();
					throw c.cancel();
				}
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
				c.replyf(FORMAT_CORRECT, SUCCESS, answer, count);
				return true;

			} else if (checkRange(c, bound, guess)) {
				history.add(guess);
				report(c, history, guess, answer, bound, count);
			}

		} catch (NumberFormatException e) {
			c.replyf(FORMAT_NAN, FAILURE);

		} catch (NumberOverflowException e) {
			c.replyf(FORMAT_OUT_OF_BOUND, FAILURE, "below or equal to " + bound);
		}
		return false;
	}

	@SuppressWarnings("null")
	private static void report(@Nonnull CommandContext c, @Nonnull MutableIntList history, int guess, int number,
							   int bound, int count) {
		if (history.size() > MAX_HISTORY)
			history.removeAtIndex(0);

		var e = new EmbedPrebuilder(LITHIUM);
		e.setFooter(FORMAT_TIP);
		e.setDescriptionf(FORMAT_STATUS, guess > number ? "lower" : "higher");
		var guesses = history.primitiveStream().mapToObj(g -> parseGuess(number, bound, g)).collect(joining("\n"));
		e.addField(format("Your guesses (%d)", count), guesses);

		c.reply(e);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseGuess(int number, int bound, int guess) {
		return format("`%s` - %s", rightPad(Integer.toString(guess), getDigits(bound)),
					  guess > number ? "lower" : "higher");
	}

	private static boolean checkRange(@Nonnull CommandContext c, int bound, int guess) {
		String limitClue = null;
		if (guess > bound)
			limitClue = "below or equal to " + bound;
		else if (guess <= 0)
			limitClue = "above 0";

		if (limitClue != null)
			c.replyf(FORMAT_OUT_OF_BOUND, FAILURE, limitClue);
		return limitClue == null;
	}

	@Override
	public String getName() {
		return "guessanumber";
	}

	@Override
	public String[] getAliases() {
		return array("guessnumber", "guessanum", "guessnum");
	}

	@Override
	public String getInfo() {
		return """
			Plays a game of "Guess My Number".""";
	}

	@Override
	public String[] getParameters() {
		return array("[max number]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("upper bound for the hidden number, 100 by default");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return GAMES;
	}

}
