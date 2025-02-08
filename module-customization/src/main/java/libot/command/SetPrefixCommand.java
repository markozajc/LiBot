//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
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

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.provider.CustomizationsProvider;

public class SetPrefixCommand extends Command {

	@Nonnull private static final Parameter PREFIX =
		optional(POSITIONAL, "prefix", "prefix to use (leave empty to reset to default)");

	public SetPrefixCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "setprefix")
			.aliases("prefix")
			.permissions(MANAGE_SERVER)
			.parameters(PREFIX)
			.description("""
				Changes LiBot's command prefix."""));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) throws Exception {
		c.arg(PREFIX).map(Argument::value).ifPresentOrElse(prefix -> {
			change(c, prefix);
		}, () -> {
			reset(c);
		});
		c.react(ACCEPT_EMOJI);
	}

	@SuppressWarnings("null")
	private static void reset(@Nonnull CommandContext c) {
		var prov = c.getProvider(CustomizationsProvider.class);
		if (prov.get(c).getCustomPrefix().isEmpty())
			throw c.error("A custom prefix is not set", DISABLED);

		if (!c.confirmf("Are you sure you want remove custom command prefix and revert it to the default one (%s)?",
						LITHIUM, monospace(c.getConfig().defaultPrefix()))) {
			throw c.cancel();
		}

		prov.get(c).setCommandPrefix(null);
	}

	private static void change(@Nonnull CommandContext c, @Nonnull String prefix) {
		if (prefix.length() > MAX_CUSTOM_PREFIX_LENGTH) {
			throw c.errorf("The maximum length of a prefix is %d characters, but yours is %d characters long!",
						   MAX_CUSTOM_PREFIX_LENGTH, prefix.length());
		}

		if (!c.confirmf("Are you sure you want to change LiBot's command prefix for this guild to %s?", LITHIUM,
						monospace(prefix))) {
			throw c.cancel();
		}

		c.getProvider(CustomizationsProvider.class).get(c.getGuildIdLong()).setCommandPrefix(prefix);
	}

}
