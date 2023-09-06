package libot.commands;

import static java.lang.Byte.compare;
import static java.lang.System.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.utils.MarkdownUtil.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import javax.annotation.*;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.commands.*;
import libot.core.commands.exceptions.CommandException;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

public class CalculatorCommand extends Command {

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

	private static final String EMOJI_INFO = "\u2139";
	private static final String EMOJI_WARN = "\u26A0";
	private static final String EMOJI_ERROR = "<:e:988959163579269130>";
	private static final String EMOJI_UNKNOWN = "\u2699";

	private static final String MODE_HIGH_PRECISION = "precision";
	private static final String MODE_EXACT = "exact";
	private static final String MODE_NORMAL = "";

	private static final byte SEPARATOR = 0x00;

	private static final byte TYPE_MESSAGE = 0x01;
	private static final byte TYPE_RESULT = 0x02;

	private static final byte LEVEL_INFO = 0x01;
	private static final byte LEVEL_WARN = 0x02;
	private static final byte LEVEL_ERROR = 0x03;

	private static final byte RESULT_APPROXIMATION = 2;

	private static final int EXIT_TIMEOUT = 102;

	private static record QalcMessage(byte level, @Nonnull String message) {}

	private static record Result(@Nonnull String value, boolean approximate) {}

	@Override
	public void execute(CommandContext c) throws InterruptedException, IOException {
		if (!ENABLED || KILL_SWITCH != null && exists(KILL_SWITCH))
			throw c.errorf("%s is unavailable.", DISABLED, c.getCommandName());

		c.typing();
		String expression = c.params().get(0);
		String mode;
		if (expression.startsWith(MODE_HIGH_PRECISION))
			mode = MODE_HIGH_PRECISION;
		else if (expression.startsWith(MODE_EXACT))
			mode = MODE_EXACT;
		else
			mode = MODE_NORMAL;

		var messages = new ArrayList<QalcMessage>(5);
		var result = evaluate(c, messages, expression, mode);

		var m = new MessageBuilder();

		if (!messages.isEmpty())
			m.setEmbeds(parseMessages(messages));

		byte[] resultFile = null;
		if (result != null) {
			String resultString = parseResult(result);
			String resultCodeblock = codeblock(resultString);
			if (resultCodeblock.length() <= Message.MAX_CONTENT_LENGTH) {
				m.setContent(resultCodeblock);

			} else {
				resultFile = resultString.getBytes(UTF_8);

				if (resultFile.length > Message.MAX_FILE_SIZE)
					throw c.error("The result is too long (> 8 MiB)", FAILURE);
			}
		}

		if (!m.isEmpty()) {
			if (resultFile != null)
				c.replyraw(m).addFile(resultFile, mode).queue();
			else
				c.reply(m);
		} else {
			if (resultFile != null)
				c.replyFile(resultFile, "result.txt");
			else
				c.reply("No output", DISABLED);
		}
	}

	@Nullable
	private static Result evaluate(@Nonnull CommandContext c, @Nonnull List<QalcMessage> messages,
								   @Nonnull String expression,
								   @Nonnull String mode) throws IOException, InterruptedException {
		byte[] output = runCalculatorProcess(c, expression, mode);

		int i = -1;
		String value = null;
		boolean approximate = false;
		while (i != output.length - 1) {
			int next = indexOf(output, SEPARATOR, i + 1);
			switch (output[i + 1]) {
				case TYPE_MESSAGE -> messages
					.add(new QalcMessage(output[i + 2], new String(output, i + 3, next - i - 3, UTF_8)));
				case TYPE_RESULT -> {
					approximate = output[i + 2] == RESULT_APPROXIMATION;
					value = new String(output, i + 3, next - i - 3, UTF_8);
				}
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
			return new Result(value, approximate);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static byte[] runCalculatorProcess(@Nonnull CommandContext c, @Nonnull String expression,
											   @Nonnull String mode) throws IOException, InterruptedException {
		var p = executeQalculate(expression.substring(mode.length()), mode);

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
	@SuppressWarnings("null")
	@SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "input is filtered")
	public static Process executeQalculate(@Nonnull String... params) throws IOException {
		var command = new String[1 + params.length];
		command[0] = getenv(ENV_QALCULATE_PATH);
		arraycopy(params, 0, command, 1, params.length);

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
	private static String parseResult(@Nonnull Result r) {
		if (r.value().contains("=") || r.value().contains("≈")) {
			if (r.approximate())
				return r.value().replace('=', '≈');
			else
				return r.value();
		} else {
			if (r.approximate())
				return "≈ " + r.value();
			else
				return "= " + r.value();
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static MessageEmbed parseMessages(@Nonnull List<QalcMessage> messages) {
		var color = switch (messages.stream().mapToInt(QalcMessage::level).max().orElse(1)) {
			case LEVEL_INFO -> LITHIUM;
			case LEVEL_WARN -> WARN;
			default -> FAILURE;
		};
		var text = messages.stream()
			.sorted((m1, m2) -> compare(m1.level(), m2.level()))
			.map(e -> "%s %s".formatted(switch (e.level()) {
				case LEVEL_INFO -> EMOJI_INFO;
				case LEVEL_WARN -> EMOJI_WARN;
				case LEVEL_ERROR -> EMOJI_ERROR;
				default -> EMOJI_UNKNOWN;
			}, monospace(e.message())))
			.collect(joining("\n"));

		return new EmbedPrebuilder(text, color).build();
	}

	@Nonnull
	private static CommandException fatal(@Nonnull CommandContext c) {
		return c.error("Failed to evaluate the expression", FAILURE);
	}

	@Override
	public String getName() {
		return "calculator";
	}

	@Override
	public String[] getAliases() {
		return array("calc", "c", "calculate", "qalc");
	}

	@Override
	@SuppressWarnings("null")
	public String getInfo() {
		return """
			Evaluates an expression. Check out the lists of supported \
			[functions](https://qalculate.github.io/manual/qalculate-definitions-functions.html), \
			[units](https://qalculate.github.io/manual/qalculate-definitions-units.html), and \
			[constants](https://qalculate.github.io/manual/qalculate-definitions-variables.html). \
			Begin your expression with `%s` for high precision mode, or `%s` for exact evaluation mode.
			Powered by [Qalculate!](https://qalculate.github.io/)
			_Note: Qalculate! input is multi-line. You can specify variable assignments (x := y) on separate lines._
			""".formatted(MODE_HIGH_PRECISION, MODE_EXACT);
	}

	@Override
	public String[] getParameters() {
		return array("[mode]", "expression");
	}

	@Override
	public String[] getParameterInfo() {
		return array("evaluation mode (precision/exact)", "expression to evaluate");
	}

	@Override
	public int getMaxParameters() {
		return 1;
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
