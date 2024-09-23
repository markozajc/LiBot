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
package libot.module;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;
import static org.apache.commons.lang3.StringUtils.capitalize;

import javax.annotation.Nonnull;

import libot.core.commands.Command;
import libot.core.entities.CommandContext;
import libot.providers.CustomizationsProvider;
import net.dv8tion.jda.api.Permission;

public class ModuleLibotShared {

	@SuppressWarnings("null")
	public static void sendUsage(@Nonnull CommandContext c, @Nonnull Command cmd) {
		// `Usage . . .` 6sp
		// `Category. .` 3sp
		// `Alias . . .` 6sp
		// `Aliases . .` 4sp
		// `Permission ` 1sp
		// `Permissions` 0sp
		// `Ratelimit .` 2sp

		var b = new StringBuilder();
		b.append("**`Usage      `** ");
		b.append(cmd.getUsage(c).replace("\n", "\n  "));

		var aliases = cmd.getAliases();
		if (!aliases.isEmpty()) {
			b.append("\n**`Alias");
			if (aliases.size() == 1)
				b.append("  ");
			else
				b.append("es");
			b.append("    `** ");
			b.append(aliases.stream().collect(joining("_, _", "_", "_")));
		}

		b.append("\n**`Category   `** _");
		b.append(capitalize(cmd.getCategory().toString().toLowerCase()));
		b.append("_");

		if (!cmd.getPermissions().isEmpty()) {
			b.append("\n**`Permission");
			if (cmd.getPermissions().size() == 1)
				b.append("s");
			else
				b.append(" ");
			b.append("`** ");
			b.append(cmd.getPermissions().stream().map(Permission::getName).collect(joining("_, _", "_", "_")));
		}

		if (cmd.getRatelimit() != 0) {
			b.append("\n**`Ratelimit  `** _1 time per ");
			long seconds = MILLISECONDS.toSeconds(cmd.getRatelimit());
			b.append(seconds);
			b.append(" second");
			if (seconds != 1)
				b.append('s');
			b.append('_');
		}

		cmd.getDescription().ifPresent(description -> {
			b.append("\n\n");
			b.append(description);
		});

		if (cmd.getCategory() == ADMINISTRATIVE)
			b.append("\n\n_(this command (or some of its parts) can only be used by LiBot's sysadmins)_");

		c.getProvider(CustomizationsProvider.class).get(c.getGuildIdLong()).getDjRoleId().ifPresent(role -> {
			if (cmd.doesRequireDjRole())
				b.append("\n\n_(this command can only be used by members with the DJ role - <@&%d>)_".formatted(role));
		});

		c.reply("Info about " + monospace(cmd.getName()), b.toString(), LITHIUM);
	}

	private ModuleLibotShared() {}

}
