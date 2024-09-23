//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
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

import static java.util.Objects.requireNonNullElse;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.GAMES;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.eu.zajc.akiwrapper.Akiwrapper.Answer.*;
import static org.eu.zajc.akiwrapper.Akiwrapper.Language.ENGLISH;

import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.*;

import org.eu.zajc.akiwrapper.Akiwrapper.*;
import org.eu.zajc.akiwrapper.AkiwrapperBuilder;
import org.eu.zajc.akiwrapper.core.entities.*;
import org.slf4j.*;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import net.dv8tion.jda.api.EmbedBuilder;

public class AkinatorCommand extends Command {

	@Nonnull private static final Parameter LANGUAGE =
		optional(POSITIONAL, "language", "language to play in, English by default");

	public AkinatorCommand() {
		super(CommandMetadata.builder(GAMES, "akinator").parameters(LANGUAGE).description("""
			Plays a game of [Akinator](https://en.akinator.com/), the character-guessing genie.
			Implemented using [Akiwrapper](https://github.com/markozajc/Akiwrapper)."""));
	}

	private static final Logger LOG = LoggerFactory.getLogger(AkinatorCommand.class);

	private static final String[] ANSWERS_BACK = { "b", "u", "back", "undo" };
	private static final String ANSWER_EXIT = "exit";

	private static final String AKITUDE_BASE_URL = "https://libot.eu.org/img/akitudes/%s.png";
	private static final Pattern AKITUDE_PATTERN = compile("/([^/]+)\\.png$");
	private static final Set<String> AKITUDES = Set
		.of("concentration_intense", "confiant", "deception", "etonnement", "inspiration_forte", "inspiration_legere",
			"leger_decouragement", "mobile", "serein", "surprise", "tension", "triomphe", "vrai_decouragement", "defi");

	private static final String CHEATSHEET = """

		Answer with \
		**`Y`** (yes), \
		**`N`** (no), \
		**`DK`** (don't know), \
		**`P`** (probably), \
		**`PN`** (probably not) or \
		**`B`** (back).""";

	@Override
	public void execute(CommandContext c) {
		c.typing();
		var lang = selectLanguage(c);
		var aw = new AkiwrapperBuilder().setFilterProfanity(!c.isChannelNSFW())
			.setLanguage(lang)
			.setTheme(Theme.CHARACTER)
			.build();

		for (var q = aw.getCurrentQuery(); q != null; q = aw.getCurrentQuery()) {
			if (q instanceof Question qe) {
				askQuestion(c, qe);

			} else if (q instanceof Guess g) {
				if (reviewGuess(c, g)) {
					g.confirm();
					finish(c, true);
					return;

				} else {
					g.reject();

					if (!c.confirm("Continue?")) {
						finish(c, false);
						return;
					}
				}
			}
		}

		finish(c, false);
	}

	@SuppressWarnings("null")
	@Nonnull
	private static Language selectLanguage(@Nonnull CommandContext c) {
		return c.arg(LANGUAGE).map(Argument::value).map(lang -> {
			var langs = EnumSet.allOf(Language.class);
			return langs.stream().filter(l -> l.toString().equalsIgnoreCase(lang)).findAny().orElseThrow(() -> {
				return c.errorf("Unsupported language", "Sorry, that language isn't supported. Try%s", DISABLED,
								langs.stream()
									.map(l -> capitalize(l.toString().toLowerCase()))
									.collect(joining("\n• ", "\n• ", "")));
			});
		}).orElse(ENGLISH);
	}

	@SuppressWarnings("null")
	private static void askQuestion(@Nonnull CommandContext c, @Nonnull Question q) {
		c.reply(createQuestionEmbed(q));
		boolean answered = false;
		while (!answered) {
			String response = c.ask().toLowerCase();
			Answer answer;
			if (ANSWER_EXIT.equals(response)) {
				if (c.confirm("Are you sure you want to exit?"))
					throw c.exit();

			} else if (contains(ANSWERS_BACK, response)) {
				if (q.getStep() == 0) {
					c.reply("You can't go further back");
				} else {
					c.typing();
					q.undoAnswer();
					answered = true;
				}

			} else if ((answer = parseAnswer(response)) == null) {
				c.reply(null, CHEATSHEET, EXIT_FOOTER, WARN);

			} else {
				c.typing();
				q.answer(answer);
				answered = true;
			}
		}
	}

	@Nonnull
	private static EmbedPrebuilder createQuestionEmbed(@Nonnull Question q) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setTitlef("Question #%02d", q.getStep() + 1);
		e.setDescription(q.getText());
		if (q.getStep() == 0)
			e.appendDescription(CHEATSHEET);
		e.setFooter(EXIT_FOOTER);
		e.setThumbnail(getAkitudeUrl(q.getAkitude()));
		return e;
	}

	private static boolean reviewGuess(@Nonnull CommandContext c, @Nonnull Guess g) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setTitle("Is this your character?");
		e.setThumbnail(AKITUDE_BASE_URL.formatted("confiant"));
		e.addFieldf(g.getName(), "_%s_", requireNonNullElse(g.getDescription(), "No description"));

		var image = g.getImage();
		if (image != null)
			e.setImage(image.toString());

		return c.confirm(true, e);
	}

	private static void finish(@Nonnull CommandContext c, boolean akinatorWins) {
		var e = new EmbedBuilder();
		if (akinatorWins) {
			e.setTitle("Great,");
			e.setDescription("guessed right one more time.");
			e.setColor(SUCCESS.rgb());
			e.setThumbnail(AKITUDE_BASE_URL.formatted("triomphe"));

		} else {
			e.setTitle("Bravo,");
			e.setDescription("you defeated me.");
			e.setColor(DISABLED.rgb());
			e.setThumbnail(AKITUDE_BASE_URL.formatted("deception"));
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

	private static String getAkitudeUrl(URL akitudeUrl) {
		var m = AKITUDE_PATTERN.matcher(akitudeUrl.toString());
		if (m.find()) {
			var akitude = m.group(1);
			if (AKITUDES.contains(akitude)) {
				return AKITUDE_BASE_URL.formatted(akitude);

			} else {
				LOG.warn("Unknown akitude: {}", akitude);
				return akitudeUrl.toString();
			}

		} else {
			LOG.warn("Unknown akitude format: {}", akitudeUrl);
			return akitudeUrl.toString();
		}
	}

}
