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

import static libot.core.Constants.FAILURE;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.UTILITIES;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.command.exception.runtime.NumberOverflowException;
import libot.core.entity.CommandContext;

public class DiceCommand extends Command {

	private static final long DEFAULT_SIDES = 6;

	@SuppressWarnings("null") @Nonnull private static final Parameter SIDES =
		optional(POSITIONAL, "sides", "number of sides, %d by default".formatted(DEFAULT_SIDES));

	public DiceCommand() {
		super(CommandMetadata.builder(UTILITIES, "dice").aliases("roll", "die").parameters(SIDES).description("""
			Rolls a dice."""));
	}

	private static final char BASE_EMOJI = '\u267F';
	private static final String GENERIC_EMOJI = "\uD83C\uDFB2";

	@Override
	public void execute(CommandContext c) {
		long sides = c.arg(SIDES).map(Argument::valueAsLong).orElse(DEFAULT_SIDES);

		if (sides < 1)
			throw c.error("The number of sides must be larger than 1", FAILURE);
		else if (sides == Long.MAX_VALUE)
			throw new NumberOverflowException();

		long rolled = ThreadLocalRandom.current().nextLong(1, sides + 1);
		var emoji = rolled > 6 ? GENERIC_EMOJI : Character.toString(BASE_EMOJI + (int) rolled);
		c.replyf("%s The dice has rolled on number **%d**.", emoji, rolled);
	}

}
