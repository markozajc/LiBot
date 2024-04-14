package libot.commands;

import static java.util.regex.Pattern.compile;
import static libot.commands.AboutCommand.LINKS;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.*;
import static libot.module.ModuleLibotShared.sendUsage;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.Random;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;

public class HelpCommand extends Command {

	private static final int LIST_MAX_WIDTH = 70;

	private static final Pattern HYPERLINK_REGEX = compile("\\[(.*?)\\]\\([^\\)]+\\)");

	public static final String FORMAT_TITLE_INFO = "Info about `%s`";
	private static final String FORMAT_NONEXISTANT =
		"`%s` does not (yet) exist! Please try again in approximately `%d` years!";
	private static final String FORMAT_DESCRIPTION = """
		To get detailed information about a command, use `%s [command]`.
		You can also use %s as a command prefix!
		%s""";
	public static final String FORMAT_ADMINISTRATIVE = """


		_(this command (or some of its parts) can only be used by LiBot's sysadmins)_""";

	@Override
	public void execute(CommandContext c) throws Exception {
		if (c.params().check(0))
			about(c);
		else
			list(c);
	}

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
				b.append("`");
				b.append(rightPad(cmd.getName(), maxLength));
				b.append("` ");
				b.append(abbreviate(HYPERLINK_REGEX.matcher(cmd.getInfo().replace("\n", "")).replaceAll("$1"),
									LIST_MAX_WIDTH - maxLength));
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

	@SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE",
						justification = "Predictable (not random) output is needed")
	private static void about(@Nonnull CommandContext c) {
		Command cmd = c.getCommands().get(c.params().get(0));
		if (cmd == null) {
			int random = new Random(c.params().get(0).hashCode()).nextInt(1000) + 300;
			throw c.errorf(FORMAT_NONEXISTANT, DISABLED, c.params().get(0).replace('`', '\''), random);
		}

		sendUsage(c, cmd);
	}

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "man", "manual" };
	}

	@Override
	public String getInfo() {
		return """
			Direct-messages you a list of all commands. \
			To get detailed information about a command, use help along with the command's name as a parameter.""";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "[command]" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "command to describe" };
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return LIBOT;
	}

}
