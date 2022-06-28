package libot.utils;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static libot.commands.libot.HelpCommand.FORMAT_TITLE_INFO;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.function.Predicate;

import javax.annotation.Nonnull;

import libot.core.commands.Command;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public class DiscordUtils {

	private static final String FORMAT_ADMINISTRATIVE = """


		_(this command (or some of its parts) can only be used by LiBot's sysadmins)_""";

	public static final String LINKS = """
		**[Get LiBot](https://libot.eu.org/get/)** - \
		**[Website](https://libot.eu.org/)** - \
		**[Support guild](https://discord.gg/asDUrbR)** - \
		**[Source](https://git.zajc.eu.org/libot.git/)**""";

	private static final char NUMBER_PREFIX = '\u20E3';
	private static final char LETTER_PREFIX = '\uD83C';
	private static final char LETTER_OFFSET_CAPITAL = 65;
	private static final char LETTER_OFFSET_LOWER = 97;
	private static final char LETTER_OFFSET = 56806;
	private static final String NUMBER_TEN = "\uD83D\uDD1F";

	public static final Predicate<Member> NO_BOT = m -> !m.getUser().isBot();

	@SuppressWarnings("null")
	public static void sendUsage(@Nonnull CommandContext c, @Nonnull Command cmd) {
		var b = new StringBuilder();
		b.append("**`Usage      `** ");
		b.append(cmd.getUsage(c).replace("\n", "\n  "));

		var aliases = cmd.getAliases();
		if (aliases.length != 0) {
			b.append("\n**`Alias");
			if (aliases.length == 1)
				b.append("    ");
			else
				b.append("es  ");
			b.append("  `** ");
			b.append(stream(aliases).collect(joining("_, _", "_", "_")));
		}

		b.append("\n**`Category   `** _");
		b.append(capitalize(cmd.getCategory().toString().toLowerCase()));
		b.append("_");

		var permissions = cmd.getPermissions();
		if (permissions.length != 0) {
			b.append("\n**`Permission");
			if (permissions.length == 1)
				b.append("s");
			b.append("`** ");
			b.append(stream(permissions).map(Permission::getName).collect(joining("_, _", "_", "_")));
		}

		if (cmd.getRatelimit() != 0) {
			b.append("\n**`Ratelimit`** _1 time per ");
			b.append(cmd.getRatelimit());
			b.append(" seconds_");
		}

		b.append("\n\n");
		b.append(cmd.getInfo());

		if (cmd.getCategory() == ADMINISTRATIVE)
			b.append(FORMAT_ADMINISTRATIVE);

		c.reply(format(FORMAT_TITLE_INFO, cmd.getName()), b.toString(), LITHIUM);
	}

	@SuppressWarnings("null")
	@Nonnull
	public static final String emoji(int i) {
		StringBuilder builder = new StringBuilder();
		if (i < 10) {
			builder.append(i);
			builder.append(NUMBER_PREFIX);
		} else if (i == 10) {
			builder.append(NUMBER_TEN);
		} else {
			throw new IllegalArgumentException("Invalid number: " + i);
		}
		return builder.toString();
	}

	@SuppressWarnings("null")
	@Nonnull
	public static final String emoji(char c) {
		StringBuilder builder = new StringBuilder();
		builder.append(LETTER_PREFIX);
		if (c < LETTER_OFFSET_LOWER)
			builder.append((char) (LETTER_OFFSET - LETTER_OFFSET_CAPITAL + c));
		else
			builder.append((char) (LETTER_OFFSET - LETTER_OFFSET_LOWER + c));
		return builder.toString();
	}

	private DiscordUtils() {}

}
