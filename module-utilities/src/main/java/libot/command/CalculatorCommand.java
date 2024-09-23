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

import static java.lang.Byte.*;
import static java.lang.System.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.*;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.UTILITIES;
import static net.dv8tion.jda.api.utils.FileUpload.fromData;
import static net.dv8tion.jda.api.utils.MarkdownUtil.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

import javax.annotation.*;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.command.exception.CommandException;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class CalculatorCommand extends Command {

	@Nonnull private static final MandatoryParameter EXPRESSION = mandatory(POSITIONAL, "expression");

	public CalculatorCommand() {
		super(CommandMetadata.builder(UTILITIES, "calculator")
			.aliases("c", "calc", "calculate", "qalc")
			.parameters(EXPRESSION)
			.description("""
				Evaluates an expression. Check out the lists of supported \
				[functions](https://qalculate.github.io/manual/qalculate-definitions-functions.html), \
				[units](https://qalculate.github.io/manual/qalculate-definitions-units.html), and \
				[constants](https://qalculate.github.io/manual/qalculate-definitions-variables.html).
				Add `mode [mode]` to the expression to activate a mode. You can activate multiple modes in an expression.
				Available modes:
				- `mode nocolor` - disables ANSI color in the output
				- `mode precision` - sets output precision to 900 digits
				- `mode exact` - avoids approximation in the output
				Powered by [Qalculate!](https://qalculate.github.io/)
				_Note: Qalculate! input is multi-line. You can specify variable assignments (x := y) on separate lines._
				"""));
	}

	public static final boolean ENABLED;
	private static final Path DATA_DIRECTORY;
	private static final int TIMEOUT_EVALUATE = 15;
	private static final Logger LOG = getLogger(CalculatorCommand.class);
	private static final Path DATA_HOME;
	@Nullable private static final Path KILL_SWITCH;
	static {
		ENABLED = getenv(ENV_QALCULATE_PATH) != null;

		if (ENABLED && getenv(ENV_QALCULATE_HOME) != null) {
			DATA_HOME = get(getenv(ENV_QALCULATE_HOME));
			DATA_DIRECTORY = DATA_HOME.resolve(".local/share/qalculate/");
			KILL_SWITCH = DATA_DIRECTORY.resolve("killswitch");

		} else {
			DATA_HOME = null;
			DATA_DIRECTORY = null;
			KILL_SWITCH = null;

			if (ENABLED)
				LOG.warn("{} is unset. Killswitch and conversion rate updating will be unavailable.",
						 ENV_QALCULATE_HOME);
			else
				LOG.warn("{} is unset. Calculator will be unavailable.", ENV_QALCULATE_PATH);
		}
	}

	private static final Pattern REGEX_BASE =
		compile("(?:\\s+convert)?\\s+to\\s+(?:b(?:ase)?\\s*([\\d]+)|([^\\s]+))", UNICODE_CHARACTER_CLASS);
	private static final Pattern REGEX_MODE = compile("mode\\s+(\\p{IsLatin}+)", UNICODE_CHARACTER_CLASS);
	private static final Pattern REGEX_NEWLINES = compile("\\\\\\s*\\n\\s*", UNICODE_CHARACTER_CLASS | MULTILINE);
	private static final Pattern MESSAGE_LINE_MARKER = compile("line \\d+: |-{3,}");

	private static final String EMOJI_INFO = "\u2139\uFE0F";
	private static final String EMOJI_WARN = "\u26A0\uFE0F";
	private static final String EMOJI_ERROR = "<:e:988959163579269130>";
	private static final String EMOJI_UNKNOWN = "\u2699\uFE0F";

	private static final byte SEPARATOR = 0x00;

	private static final byte TYPE_MESSAGE = 0x01;
	private static final byte TYPE_RESULT = 0x02;

	private static final byte LEVEL_INFO = 0x01;
	private static final byte LEVEL_WARN = 0x02;
	private static final byte LEVEL_ERROR = 0x03;

	private static final int EXIT_TIMEOUT = 102;

	private static record QalcMessage(byte level, @Nonnull String message) {}

	private static record Result(@Nonnull String value) {}

	@Nonnull
	@SuppressWarnings("null")
	private static String getModes(@Nonnull String expression, @Nonnull List<QalcMessage> messages,
								   @Nonnull Set<Mode> modes) {
		return REGEX_MODE.matcher(expression).replaceAll(r -> {
			Arrays.stream(Mode.values())
				.filter(m -> m.name().equalsIgnoreCase(r.group(1)))
				.findAny()
				.ifPresentOrElse(modes::add, () -> {
					messages.add(new QalcMessage(LEVEL_WARN, "Unknown mode: " + r.group(1).toLowerCase()));
				});
			return "";
		});
	}

	@Override
	@SuppressWarnings({ "null", "resource" })
	public void execute(CommandContext c) throws InterruptedException, IOException {
		if (!ENABLED || KILL_SWITCH != null && exists(KILL_SWITCH))
			throw c.errorf("%s is unavailable.", DISABLED, c.getCommandName());

		c.typing();
		String expression = c.arg(EXPRESSION).value();
		expression = REGEX_NEWLINES.matcher(expression).replaceAll(" ").replace("\\", "");

		var messages = new ArrayList<QalcMessage>(5);
		var modes = EnumSet.noneOf(Mode.class);
		expression = getModes(expression, messages, modes);

		int base = 10;
		var baseMatcher = REGEX_BASE.matcher(expression);
		if (baseMatcher.find()) {
			base = getBase(baseMatcher);
			if (base == 0)
				base = 10; // base invalid, leave expression as-is and set base back to 10
			else
				expression = baseMatcher.replaceFirst(""); // base valid, remove conversion string
		}

		var result = evaluate(c, messages, expression, modes, base);

		var m = new MessageCreateBuilder();

		if (!messages.isEmpty())
			m.setEmbeds(parseMessages(messages));

		FileUpload resultFile = null;
		if (result != null) {
			String resultCodeblock = codeblock("ansi", result.value());
			if (resultCodeblock.length() <= Message.MAX_CONTENT_LENGTH) {
				m.setContent(resultCodeblock);

			} else {
				byte[] bytes = result.value().getBytes(UTF_8);

				if (bytes.length > Message.MAX_FILE_SIZE)
					throw c.error("The result is too long (> 25 MiB)", FAILURE);

				resultFile = fromData(bytes, "result.txt");
			}
		}

		if (!m.isEmpty()) {
			if (resultFile != null)
				c.replyraw(m).addFiles(resultFile).queue();
			else
				c.reply(m);
		} else {
			if (resultFile != null)
				c.replyFiles(resultFile);
			else
				c.reply("No output", DISABLED);
		}
	}

	public static int getMode(Matcher matcher) {
		var mode = matcher.group().toLowerCase();
		if (mode.endsWith("precision"))
			return 2;
		else
			return 3;
	}

	public static int getBase(Matcher matcher) {
		var numeric = matcher.group(1); // base N

		if (numeric != null) {
			try {
				var base = parseByte(numeric);
				if (base < 2 || base > 36) // qalculate's own limit
					return 0;
				else
					return base;

			} catch (NumberFormatException e) {
				return 0;
			}
		}

		var textual = matcher.group(2); // hex, dec, bin, ...
		return switch (textual.toLowerCase()) { // loosely copied from qalc.cc
			case "fp80" /* qalc.cc also lacks binary80 */ -> -34; // BASE_FP80
			case "fp128", "binary128" -> -33; // BASE_FP128
			case "fp64", "binary64", "double" -> -32; // BASE_FP64
			case "fp32", "binary32", "float" -> -31; // BASE_FP32
			case "fp16", "binary16" -> -30; // BASE_FP16
			case "bijective" -> -26; // BASE_BIJECTIVE_26
			case "bcd" -> -20;
			case "unicode" -> -4; // BASE_UNICODE
			case "time" -> -2; // BASE_TIME
			case "roman" -> -1; // BASE_ROMAN_NUMERALS
			case "bin", "binary" -> 2;
			case "oct", "octal" -> 8;
			case "dec", "decimal" -> 10;
			case "doz", "dozenal", "duo", "duodecimal" -> 12;
			case "hex", "hexadecimal" -> 16;
			case "sexa", "sexagesimal" -> 60; // BASE_SEXAGESIMAL
			case "sexa2", "sexagesimal2" -> 62; // BASE_SEXAGESIMAL_2
			case "sexa3", "sexagesimal3" -> 63; // BASE_SEXAGESIMAL_3
			case "latitude" -> 70; // BASE_LATITUDE
			case "latitude2" -> 71; // BASE_LATITUDE_2
			case "longitude" -> 72; // BASE_LONGITUDE
			case "longitude2" -> 73; // BASE_LONGITUDE_2
			default -> 0;
		};
	}

	@Nullable
	private static Result evaluate(@Nonnull CommandContext c, @Nonnull List<QalcMessage> messages, String expression,
								   @Nonnull Set<Mode> modes, int base) throws IOException, InterruptedException {
		byte[] output = runCalculatorProcess(c, expression, modes, base);

		int i = -1;
		String value = null;
		while (i != output.length - 1) {
			int next = indexOf(output, SEPARATOR, i + 1);
			switch (output[i + 1]) {
				case TYPE_RESULT -> value = new String(output, i + 2, next - i - 2, UTF_8);
				case TYPE_MESSAGE -> messages
					.add(new QalcMessage(output[i + 2], new String(output, i + 3, next - i - 3, UTF_8)));

				default -> {
					if (LOG.isErrorEnabled())
						LOG.error("Unparsable output! (base64): {}", Base64.getEncoder().encodeToString(output));
					throw fatal(c);
				}
			}
			i = next == -1 ? output.length - 1 : next;
		}
		if (value == null)
			return null;
		else
			return new Result(value);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static byte[] runCalculatorProcess(@Nonnull CommandContext c, String expression, @Nonnull Set<Mode> modes,
											   int base) throws IOException, InterruptedException {
		var p = executeQalculate(expression, Integer.toString(Mode.toBits(modes)), Integer.toString(base));

		if (!p.waitFor(TIMEOUT_EVALUATE, SECONDS) || p.exitValue() == EXIT_TIMEOUT) {
			if (p.isAlive())
				p.destroyForcibly();

			if (LOG.isWarnEnabled())
				LOG.warn("Expression timed out: (base64) {}",
						 Base64.getEncoder().encodeToString(expression.getBytes(UTF_8)));

			throw c.error("Evaluation took too long, please use a simpler expression", DISABLED);
		}

		if (p.exitValue() != 0) {
			if (LOG.isWarnEnabled())
				LOG.error("Expression caused non-zero exit {}: (base64) {}", p.exitValue(),
						  Base64.getEncoder().encodeToString(expression.getBytes(UTF_8)));

			throw fatal(c);
		}

		try (var stdout = p.getInputStream()) {
			return toByteArray(stdout);
		}
	}

	@Nonnull
	private static Process executeQalculate(@Nonnull String... params) throws IOException {
		var command = new String[1 + params.length];
		command[0] = getenv(ENV_QALCULATE_PATH);
		arraycopy(params, 0, command, 1, params.length);
		return startProcess(command);
	}

	@Nonnull
	@SuppressWarnings("null")
	@SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "input is filtered")
	public static Process startProcess(@Nonnull String... command) throws IOException {
		var b = new ProcessBuilder(command);
		b.environment().clear();
		if (getenv(ENV_QALCULATE_HOME) != null) {
			b.environment().put("HOME", getenv(ENV_QALCULATE_HOME));
			b.environment().put("ENV", getenv(ENV_QALCULATE_HOME) + "/.profile");
		}
		if (DATA_HOME != null)
			b.directory(DATA_HOME.toFile());
		return b.start();
	}

	@Nonnull
	@SuppressWarnings("null")
	private static MessageEmbed parseMessages(@Nonnull List<QalcMessage> messages) {
		var color = switch (messages.stream().mapToInt(QalcMessage::level).max().orElse(1)) {
			case LEVEL_INFO -> LITHIUM;
			case LEVEL_WARN -> WARN;
			default -> FAILURE;
		};
		var text = messages.stream().sorted((m1, m2) -> compare(m1.level(), m2.level())).map(e -> {
			if (e.level == LEVEL_INFO && e.message.contains("\n")) {
				return codeblock(MESSAGE_LINE_MARKER.matcher(e.message).replaceAll(""));

			} else {
				return "%s %s".formatted(switch (e.level()) {
					case LEVEL_INFO -> EMOJI_INFO;
					case LEVEL_WARN -> EMOJI_WARN;
					case LEVEL_ERROR -> EMOJI_ERROR;
					default -> EMOJI_UNKNOWN;
				}, monospace(e.message()));
			}
		}).collect(joining("\n"));

		return new EmbedPrebuilder(text, color).build();
	}

	@Nonnull
	private static CommandException fatal(@Nonnull CommandContext c) {
		return c.error("Failed to evaluate the expression", FAILURE);
	}

	public enum Mode {

		PRECISION(0),
		EXACT(1),
		NOCOLOR(2);

		private final int bit;

		Mode(int bit) {
			this.bit = 1 << bit;
		}

		public int getBit() {
			return this.bit;
		}

		public static int toBits(@Nonnull Collection<Mode> modes) {
			return modes.stream().mapToInt(Mode::getBit).reduce((m1, m2) -> m1 | m2).orElse(0);
		}

	}

}
