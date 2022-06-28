package libot.commands.utilities;

import static java.lang.Byte.compare;
import static java.lang.System.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;
import static java.time.Instant.ofEpochMilli;
import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.utils.MarkdownUtil.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Stream;

import javax.annotation.*;

import org.apache.commons.lang3.mutable.MutableLong;
import org.eu.zajc.functions.exceptionable.all.AEFunction;
import org.slf4j.Logger;

import libot.core.commands.*;
import libot.core.commands.exceptions.CommandException;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

public class CalculatorCommand extends Command {

	private static final int TIMEOUT_EVALUATE = 15;
	private static final int TIMEOUT_UPDATE = 35;
	private static final Logger LOG = getLogger(CalculatorCommand.class);
	private static final MutableLong LAST_RATES_UPDATE;
	private static final long MAX_RATES_DATE = DAYS.toMillis(6);
	private static final Path DATA_HOME = get(getenv(ENV_QALCULATE_HOME));
	private static final Path DATA_DIRECTORY = DATA_HOME.resolve(".local/share/qalculate/");
	private static final Path KILL_SWITCH = DATA_DIRECTORY.resolve("killswitch");
	static {
		long mtime;
		try {
			mtime = Stream.of("btc.json", "eurofxref-daily.xml", "rates.html")
				.map(DATA_DIRECTORY::resolve)
				.map((AEFunction<Path, FileTime>) Files::getLastModifiedTime)
				.mapToLong(FileTime::toMillis)
				.min()
				.orElse(0);
		} catch (Exception e) {
			if (!(e instanceof NoSuchFileException))
				LOG.error("Could not determine exchange rates mtime", e);
			mtime = 0;
		}

		if (LOG.isDebugEnabled())
			LOG.debug("Determined last rates update as {}", ofEpochMilli(mtime));
		LAST_RATES_UPDATE = new MutableLong(mtime);
	}

	private static final String EMOJI_INFO = "\u2139";
	private static final String EMOJI_WARN = "\u26A0";
	private static final String EMOJI_ERROR = "<:e:988959163579269130>";
	private static final String EMOJI_UNKNOWN = "\u2699";

	private static final String MODE_HIGH_PRECISION = "precision";
	private static final String MODE_EXACT = "exact";
	private static final String MODE_NORMAL = "";

	private static final byte SEPARATOR = 0;

	private static final byte TYPE_MESSAGE = 1;
	private static final byte TYPE_RESULT = 2;

	private static final byte LEVEL_INFO = 1;
	private static final byte LEVEL_WARN = 2;
	private static final byte LEVEL_ERROR = 3;

	private static final byte RESULT_APPROXIMATION = 2;

	private static final int EXIT_TIMEOUT = 102;

	private static record QalcMessage(byte level, @Nonnull String message) {}

	private static record Result(@Nonnull String value, boolean approximate) {}

	@Override
	public void execute(CommandContext c) throws InterruptedException, IOException {
		if (exists(KILL_SWITCH))
			throw c.errorf("%s is disabled.", DISABLED, c.getCommandName());

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
		synchronized (LAST_RATES_UPDATE) {
			long now = currentTimeMillis();
			if (now - LAST_RATES_UPDATE.longValue() > MAX_RATES_DATE && updateRates()) {
				messages.add(new QalcMessage(LEVEL_INFO, "Updated exchange rates."));
				LAST_RATES_UPDATE.setValue(now);
			}
		}

		Result result;
		result = evaluate(c, messages, expression, mode);

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

		if (m.isEmpty() && resultFile != null)
			c.reply("No output", DISABLED);
		else if (resultFile == null)
			c.reply(m.build());
		else
			c.replyraw(m.build()).addFile(resultFile, "result.txt").submit();
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
		var p = exec(getenv(ENV_QALCULATE_PATH), expression.substring(mode.length()), mode);

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

	private static boolean updateRates() throws IOException, InterruptedException {
		var p = exec(getenv(ENV_QALCULATE_PATH), "update");

		if (!p.waitFor(TIMEOUT_UPDATE, SECONDS)) {
			if (p.isAlive())
				p.destroyForcibly();

			LOG.warn("Updating exchange rates timed out");
			return false;
		}

		if (p.exitValue() != 0) {
			LOG.warn("Updating rates caused non-zero exit {}", p.exitValue());
			return false;
		}

		return true;
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Process exec(@Nonnull String... command) throws IOException {
		var b = new ProcessBuilder(command);
		b.environment().clear();
		b.environment().put("HOME", getenv(ENV_QALCULATE_HOME));
		b.environment().put("ENV", getenv(ENV_QALCULATE_HOME) + "/.profile");
		b.directory(DATA_HOME.toFile());
		return b.start();
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseResult(@Nonnull Result r) {
		if (r.value().indexOf('=') != -1) {
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
		return array("calc", "c", "calculate");
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
			_Note: Qalculate! input is single-line. Newlines are ignored if multi-line input is provided._
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
