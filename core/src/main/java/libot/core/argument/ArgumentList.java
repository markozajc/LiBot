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
package libot.core.argument;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.*;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.*;
import libot.util.ParseUtils;

public class ArgumentList {

	@SuppressWarnings("null") static final ArgumentList EMPTY = new ArgumentList(emptyMap());

	@Nonnull private final Map<Parameter, Argument> arguments;

	ArgumentList(@Nonnull Map<Parameter, Argument> arguments) {
		this.arguments = arguments;
	}

	@Nonnull
	public Argument get(@Nonnull MandatoryParameter param) {
		return requireNonNull(this.arguments.get(param));
	}

	@Nonnull
	@SuppressWarnings("null")
	public Optional<Argument> get(@Nonnull Parameter param) {
		return Optional.ofNullable(this.arguments.get(param));
	}

	public static record Argument(String value) {

		@SuppressWarnings("null")
		public int valueAsInt() {
			return ParseUtils.parseInt(this.value);
		}

		@SuppressWarnings("null")
		public long valueAsLong() {
			return ParseUtils.parseLong(this.value);
		}

	}

}
