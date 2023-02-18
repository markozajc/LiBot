package libot.commands;

import static com.markozajc.akiwrapper.Akiwrapper.Answer.*;
import static com.markozajc.akiwrapper.core.entities.Server.GuessType.CHARACTER;
import static com.markozajc.akiwrapper.core.entities.Server.Language.ENGLISH;
import static java.lang.Double.compare;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.utils.Utilities.array;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.*;

import javax.annotation.*;

import org.eclipse.collections.api.set.primitive.*;
import org.eclipse.collections.impl.factory.primitive.LongSets;

import com.markozajc.akiwrapper.*;
import com.markozajc.akiwrapper.Akiwrapper.Answer;
import com.markozajc.akiwrapper.core.entities.*;
import com.markozajc.akiwrapper.core.entities.Server.Language;
import com.markozajc.akiwrapper.core.exceptions.*;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.EmbedBuilder;

public class AkinatorCommand extends Command {

	private static final String TIMEOUT_ERROR = "KO - TIMEOUT";
	private static final String FORMAT_INTERRUPTED = """
		Looks like the Akinator server you were playing on has stopped working.""";
	private static final String FORMAT_CHEAT_SHEET = """

		Answer with \
		**`Y`** (yes), \
		**`N`** (no), \
		**`DK`** (don't know), \
		**`P`** (probably), \
		**`PN`** (probably not) or \
		**`B`** (back).""";
	private static final String TITLE_DEBUG = "Debug information";
	private static final String FORMAT_DEBUG = """
		**`session health:. . . . `** %s
		**`server                 `**
		**` ┣ url:  . . . . . . . `** %s
		**` ┣ language: . . . . . `** %s
		**` ┗ type: . . . . . . . `** %s
		**`time                   `**
		**` ┣ since start:  . . . `** %d
		**` ┗ since last request: `** %d
		**`progression            `**
		**` ┣ old:  . . . . . . . `** %.3f
		**` ┗ current:  . . . . . `** %.3f
		**`guess count: . . . . . `** %d""";
	private static final String FORMAT_SERVERS_DOWN = """
		Currently, all Akinator's servers for language %s are unavailable.
		**Suggestions:**
		• Retry after a while
		• Use another language""";
	private static final String FORMAT_LANGUAGE_UNSUPPORTED = "Sorry, that language isn't supported. Try%s";

	private static final String[] ANSWERS_BACK = array("b", "u", "back", "undo");
	private static final String ANSWER_DEBUG = "debug";
	private static final String ANSWER_EXIT = "exit";

	private static final String EMOTE_BASE_URL = "https://libot.eu.org/img/akitudes/%s.webp";

	private static final double GUESS_THRESHOLD = 0.85D;

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.typing();
		var lang = selectLanguage(c);
		var aw = constructAkiwrapper(c, lang);
		long startTimestamp = currentTimeMillis();
		long lastTimestamp = currentTimeMillis();
		boolean finished = false;
		double oldProgress = 0;
		double progression = 0;
		try {
			var rejected = LongSets.mutable.empty();
			for (var q = aw.getCurrentQuestion(); q != null; q = aw.getCurrentQuestion()) {
				askQuestion(c, aw, q, startTimestamp, lastTimestamp, rejected, oldProgress, progression);
				oldProgress = progression;
				progression = q.getProgression();
				if (checkGuesses(c, aw, rejected)) {
					finished = true;
					break;
				}
				lastTimestamp = currentTimeMillis();
			}

			if (!finished)
				reviewFinal(c, aw);

		} catch (ServerUnavailableException e) {
			throw c.error(FORMAT_INTERRUPTED, DISABLED);

		} catch (StatusException e) {
			if (TIMEOUT_ERROR.equals(e.getMessage()))
				throw c.error("Session has expired.", DISABLED);
			else
				throw e;
		}

	}

	@SuppressWarnings("null")
	@Nonnull
	private static Language selectLanguage(@Nonnull CommandContext c) {
		if (c.params().check(0)) {
			var langs = EnumSet.allOf(Language.class);
			return langs.stream()
				.filter(l -> l.toString().equalsIgnoreCase(c.params().get(0)))
				.findAny()
				.orElseThrow(() -> c.errorf("Unsupported language", FORMAT_LANGUAGE_UNSUPPORTED, DISABLED,
											langs.stream()
												.map(l -> capitalize(l.toString().toLowerCase()))
												.collect(joining("\n• ", "\n• ", ""))));
		} else {
			return ENGLISH;
		}
	}

	@Nonnull
	private static Akiwrapper constructAkiwrapper(@Nonnull CommandContext c, @Nonnull Language lang) {
		try {
			return new AkiwrapperBuilder().setFilterProfanity(!c.isChannelNSFW())
				.setLanguage(lang)
				.setGuessType(CHARACTER)
				.build();
		} catch (ServerUnavailableException | ServerNotFoundException e) {
			throw c.errorf(FORMAT_SERVERS_DOWN, DISABLED, capitalize(lang.toString().toLowerCase()));
		}
	}

	@SuppressWarnings("null")
	private static void askQuestion(@Nonnull CommandContext c, @Nonnull Akiwrapper aw, @Nonnull Question q,
									long startTimestamp, long lastTimestamp, LongSet rejected, double oldProgression,
									double progression) {
		c.reply(createQuestionEmbed(q, oldProgression, progression));
		boolean answered = false;
		while (!answered) {
			String response = c.ask().toLowerCase();
			Answer answer;
			if (ANSWER_EXIT.equals(response)) {
				if (c.confirm("Are you sure you want to exit?"))
					throw c.exit();

			} else if (ANSWER_DEBUG.equals(response)) {
				sendDebug(c, aw, startTimestamp, lastTimestamp, rejected, oldProgression, progression);

			} else if (contains(ANSWERS_BACK, response)) {
				if (q.getStep() == 0) {
					c.reply("You can't go further back");
				} else {
					c.typing();
					aw.undoAnswer();
					answered = true;
				}

			} else if ((answer = parseAnswer(response)) == null) {
				c.reply(null, FORMAT_CHEAT_SHEET, EXIT_FOOTER, WARN);

			} else {
				c.typing();
				aw.answerCurrentQuestion(answer);
				answered = true;
			}
		}
	}

	@SuppressWarnings("null")
	private static void sendDebug(@Nonnull CommandContext c, @Nonnull Akiwrapper aw, long startTimestamp,
								  long lastTimestamp, @Nonnull LongSet rejected, double oldProgression,
								  double progression) {
		List<Guess> guesses = null;
		Exception ex = null;
		try {
			guesses = aw.getGuesses();
		} catch (Exception e1) {
			ex = e1;
		}

		var de = new EmbedPrebuilder(LITHIUM);
		de.setTitle(TITLE_DEBUG);
		de.appendDescriptionf(FORMAT_DEBUG, ex == null ? "OK (via guesses route)" : "KO (%s)".formatted(ex.toString()),
							  aw.getServer().getUrl(), aw.getServer().getLanguage().toString().toLowerCase(),
							  aw.getServer().getGuessType().toString().toLowerCase(),
							  currentTimeMillis() - startTimestamp, currentTimeMillis() - lastTimestamp, oldProgression,
							  progression, guesses == null ? 0 : guesses.size());

		if (guesses != null && !guesses.isEmpty()) {
			de.addField("Guesses (threshold = %.3f)".formatted(GUESS_THRESHOLD),
						guesses.stream()
							.map(g -> "`%.3f` %s%s [%d]".formatted(g.getProbability(),
																   rejected.contains(g.getIdLong()) ? "(rejected) "
																	   : "",
																   g.getName(), g.getIdLong()))
							.collect(joining("\n")));
		}

		if (!rejected.isEmpty())
			de.addField("Rejected guess IDs", rejected.makeString("`", "`, `", "`"));

		c.reply(de);
	}

	@Nonnull
	private static EmbedPrebuilder createQuestionEmbed(@Nonnull Question q, double oldProgression, double progression) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setTitle("Question #%02d".formatted(q.getStep() + 1));
		e.setDescription(q.getQuestion());
		if (q.getStep() == 0)
			e.appendDescription(FORMAT_CHEAT_SHEET);
		e.setFooter(EXIT_FOOTER);
		e.setThumbnail(getEmoteUrl(q.getStep(), progression, oldProgression));
		return e;
	}

	private static boolean checkGuesses(@Nonnull CommandContext c, @Nonnull Akiwrapper aw,
										@Nonnull MutableLongSet rejected) {
		boolean anyQuestionRejected = false;
		for (var g : aw.getGuessesAboveProbability(GUESS_THRESHOLD)
			.stream()
			.sorted((g1, g2) -> compare(g2.getProbability(), g1.getProbability()))
			.toList()) {
			if (!rejected.contains(g.getIdLong())) {
				if (reviewGuess(c, g)) {
					finish(c, true);
					return true;

				} else {
					anyQuestionRejected = true;
					rejected.add(g.getIdLong());
				}
			}
		}

		if (anyQuestionRejected && !c.confirm("Continue?")) {
			finish(c, false);
			return true;

		} else {
			return false;
		}
	}

	@SuppressWarnings("null")
	private static void reviewFinal(@Nonnull CommandContext c, @Nonnull Akiwrapper aw) {
		boolean anyConfirmed = false;
		for (var guess : aw.getGuesses()) {
			if (reviewGuess(c, guess)) {
				finish(c, true);
				anyConfirmed = true;
				break;
			}
		}
		if (!anyConfirmed)
			finish(c, false);
	}

	private static boolean reviewGuess(@Nonnull CommandContext c, @Nonnull Guess g) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setTitle("Is this your character?");
		e.setThumbnail(EMOTE_BASE_URL.formatted("confiant"));
		e.addFieldf(g.getName(), "_%s_", requireNonNullElse(g.getDescription(), "No description"));

		var image = g.getImage();
		if (image != null)
			e.setImage(image.toString());

		return c.confirm(true, e);
	}

	private static void finish(@Nonnull CommandContext c, boolean win) {
		var e = new EmbedBuilder();
		if (win) {
			e.setTitle("Great,");
			e.setDescription("guessed right one more time.");
			e.setColor(SUCCESS);
			e.setThumbnail(EMOTE_BASE_URL.formatted("triomphe"));
		} else {
			e.setTitle("Bravo,");
			e.setDescription("you defeated me.");
			e.setColor(DISABLED);
			e.setThumbnail(EMOTE_BASE_URL.formatted("deception"));
		}
		c.reply(e);
	}

	@Nullable
	private static Answer parseAnswer(@Nonnull String answer) {
		return switch (answer) {
			case "y", "yes" -> YES;
			case "n", "no" -> NO;
			case "dk", "idk", "dontknow", "dont know", "don't know" -> DONT_KNOW;
			case "p", "probably" -> PROBABLY;
			case "pn", "probably no", "probably not" -> PROBABLY_NOT;
			default -> null;
		};
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String getEmoteUrl(int step, double progression, double oldProgression) {
		// taken and translated directly from akinator (workflow_game_desktop.js)
		int progressionTarget = step * 4;
		double progressionWeighed;
		if (step <= 10)
			progressionWeighed = (step * progression + (10 - step) * progressionTarget) / 10;
		else
			progressionWeighed = 0;

		String name;
		if (progression >= 80)
			name = "mobile";
		else if (oldProgression < 50 && progression >= 50)
			name = "inspiration_forte";
		else if (progression >= 50)
			name = "confiant";
		else if (oldProgression - progression > 16)
			name = "surprise";
		else if (progressionWeighed - progression > 8)
			name = "etonnement";
		else if (progressionWeighed >= progressionTarget)
			name = "inspiration_legere";
		else if (progressionWeighed >= progressionTarget * .8D)
			name = "serein";
		else if (progressionWeighed >= progressionTarget * .6D)
			name = "concentration_intense";
		else if (progressionWeighed >= progressionTarget * .4D)
			name = "leger_decouragement";
		else if (progressionWeighed >= progressionTarget * .2D)
			name = "tension";
		else
			name = "vrai_decouragement";
		return EMOTE_BASE_URL.formatted(name);
	}

	@Override
	public String getName() {
		return "akinator";
	}

	@Override
	public String getInfo() {
		return """
			Plays a game of [Akinator](https://en.akinator.com/), the character-guessing genie.
			Implemented using [Akiwrapper](https://github.com/markozajc/Akiwrapper).""";
	}

	@Override
	public String[] getParameters() {
		return array("[language]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("language to play in, English by default");
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
