package libot.module;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static org.apache.commons.lang3.StringUtils.capitalize;

import javax.annotation.Nonnull;

import libot.commands.HelpCommand;
import libot.core.commands.Command;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class ModuleLibotShared {

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
			b.append(HelpCommand.FORMAT_ADMINISTRATIVE);

		c.reply(format(HelpCommand.FORMAT_TITLE_INFO, cmd.getName()), b.toString(), LITHIUM);
	}

	private ModuleLibotShared() {}

}
