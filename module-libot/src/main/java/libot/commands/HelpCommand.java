package libot.commands;

import static java.util.regex.Pattern.compile;
import static libot.commands.AboutCommand.LINKS;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.*;
import static libot.module.ModuleLibotShared.sendUsage;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.Random;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;

public class HelpCommand extends Command {

	@Nonnull private static final Parameter COMMAND = optional(POSITIONAL, "command", "The command to describe");

	public HelpCommand() {
		super(CommandMetadata.builder(LIBOT, "help")
			.aliases("man", "manual")
			.parameters(COMMAND)
			.description("""
				Direct-messages you a list of all commands. \
				To get detailed information about a command, use help along with the command's name as a parameter.""")
			.build());
	}

	private static final int LIST_MAX_WIDTH = 70;
	private static final Pattern HYPERLINK_REGEX = compile("\\[(.*?)\\]\\([^\\)]+\\)");

	private static final String FORMAT_NONEXISTANT =
		"`%s` does not (yet) exist! Please try again in approximately `%d` years!";
	private static final String FORMAT_DESCRIPTION = """
		To get detailed information about a command, use `%s [command]`.
		You can also use %s as a command prefix!
		%s""";

	@Override
	public void execute(CommandContext c) throws Exception {
		c.arg(COMMAND).ifPresentOrElse(arg -> about(c, arg), () -> list(c));
	}

	@SuppressWarnings("null")
	private static void list(@Nonnull CommandContext c) {
		var e = new EmbedPrebuilder("LiBot manual", LITHIUM);
		e.setFooter("LiBot v" + VERSION, c.getSelfAvatar());

		int maxLength =
			c.getCommands().getAll().stream().map(Command::getName).mapToInt(String::length).max().orElse(0);
		var b = new StringBuilder();
		for (var category : CommandCategory.values()) {
			if (category == ADMINISTRATIVE && !c.isUserSysadmin())
				continue;

			b.setLength(0);
			for (Command cmd : c.getCommands().getInCategory(category)) {
				b.append(monospace(rightPad(cmd.getName(), maxLength)));

				cmd.getDescription().map(desc -> {
					return abbreviate(HYPERLINK_REGEX.matcher(desc.replace("\n", "")).replaceAll("$1"),
									  LIST_MAX_WIDTH - maxLength);
				}).ifPresent(b::append);

				b.append("\n");
			}
			e.addField(category.toString(), b.toString(), false);
		}

		e.setDescriptionf(FORMAT_DESCRIPTION, c.getCommandWithPrefix(), c.getSelfMention(), LINKS);

		c.direct(e).thenAcceptAsync(m -> c.react(ACCEPT_EMOJI)).exceptionally(t -> {
			c.reply(e);
			return null;
		});
	}

	@SuppressWarnings("null")
	@SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "Deterministic output is needed")
	private static void about(@Nonnull CommandContext c, Argument command) {
		c.getCommands().get(command.value()).ifPresentOrElse(cmd -> {
			sendUsage(c, cmd);
		}, () -> {
			int random = new Random(command.value().hashCode()).nextInt(1000) + 300;
			throw c.errorf(FORMAT_NONEXISTANT, DISABLED, command.value().replace('`', '\''), random);
		});
	}

}
