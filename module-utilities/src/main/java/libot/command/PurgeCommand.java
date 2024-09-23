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

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.UTILITIES;
import static libot.util.Utilities.plural;
import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.command.exception.ExceptionHandler;
import libot.core.entity.CommandContext;

public class PurgeCommand extends Command {

	@Nonnull private static final MandatoryParameter COUNT =
		mandatory(POSITIONAL, "count", "number of messages to purge");

	public PurgeCommand() {
		super(CommandMetadata.builder(UTILITIES, "purge")
			.aliases("prune")
			.permissions(MESSAGE_MANAGE)
			.parameters(COUNT)
			.description("Purges/deletes messages from a channel."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		int quantity = c.arg(COUNT).valueAsInt();
		if (0 >= quantity)
			throw c.error("The number of messages to purge must not be negative.", FAILURE);

		c.getChannel().getIterableHistory().takeAsync(quantity + 1).thenApply(l -> {
			c.getChannel().purgeMessages(l);
			return l.size();
		}).thenAccept(n -> c.replyf("Purged **%d** message%s", LITHIUM, n - 1, plural(n - 1))).exceptionally(e -> {
			ExceptionHandler.handle(e, c);
			return null;
		});
	}

}
