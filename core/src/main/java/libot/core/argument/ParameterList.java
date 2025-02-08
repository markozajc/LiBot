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
package libot.core.argument;

import static com.google.common.collect.Streams.concat;
import static java.util.Arrays.*;
import static java.util.Collections.emptyMap;
import static libot.core.argument.ParameterList.Parameter.ParameterType.NAMED;

import java.util.*;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.annotation.*;

import org.apache.commons.lang3.mutable.*;

import com.google.common.collect.Maps;

import libot.core.argument.ArgumentList.Argument;

public class ParameterList {

	@SuppressWarnings("null")
	@Nonnull static final ParameterList EMPTY = new ParameterList(new Parameter[0], emptyMap(), -1, new Parameter[0]);

	@Nonnull private final Parameter[] positional;
	@Nonnull private final Map<String, Parameter> named;
	private final int positionalRequiredIndex;
	@Nonnull private final Parameter[] namedRequired;

	public ParameterList(@Nonnull Parameter[] positional, @Nonnull Map<String, Parameter> named, int positionalRequired,
						 @Nonnull Parameter[] namedRequired) {
		this.positional = positional;
		this.named = named;
		this.positionalRequiredIndex = positionalRequired;
		this.namedRequired = namedRequired;
	}

	@Nonnull
	public static ParameterList empty() {
		return EMPTY;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static ParameterList of(@Nonnull Parameter... parameters) {
		return of(asList(parameters));
	}

	@Nonnull
	@SuppressWarnings("null")
	public static ParameterList of(@Nonnull List<Parameter> parameters) {
		if (parameters.isEmpty())
			return EMPTY;

		var named = Maps.<String, Parameter>newHashMapWithExpectedSize(parameters.size());
		var namedRequired = new ArrayList<Parameter>(parameters.size());
		var positional = new ArrayList<Parameter>(parameters.size());
		for (var param : parameters) {
			switch (param.getType()) {
				case NAMED -> {
					named.put(param.getName(), param);
					if (param.isMandatory())
						namedRequired.add(param);
				}
				case POSITIONAL -> positional.add(param);

			}
		}
		positional.trimToSize();
		namedRequired.trimToSize();

		int positionalRequiredIndex = -1;
		for (int i = positional.size(); i-- > 0;) {
			if (positional.get(i).isMandatory()) {
				positionalRequiredIndex = i;
				break;
			}
		}

		return new ParameterList(positional.toArray(Parameter[]::new), new HashMap<>(named), positionalRequiredIndex,
								 namedRequired.toArray(Parameter[]::new));
	}

	@Nonnull
	@SuppressWarnings("null")
	public ArgumentList parse(@Nullable String input) {
		var positionalIndex = new MutableInt(0);

		Map<Parameter, Argument> arguments;
		if (input == null || input.length() == 0)
			arguments = emptyMap();
		else
			arguments = parseArgumentMap(input, positionalIndex);

		if (positionalIndex.intValue() - 1 < this.positionalRequiredIndex)
			throw new UsageException("Missing argument: " + this.positional[this.positionalRequiredIndex].getName());

		for (var required : this.namedRequired) {
			if (!arguments.containsKey(required))
				throw new UsageException("Missing argument: --" + required.getName());
		}

		if (arguments.isEmpty())
			return ArgumentList.EMPTY;
		else
			return new ArgumentList(arguments);
	}

	@Nonnull
	@SuppressWarnings("null")
	private Map<Parameter, Argument> parseArgumentMap(@Nonnull String input, @Nonnull MutableInt positionalIndex) {
		if (this == EMPTY)
			throw new UsageException("Too many arguments");

		var arguments =
			Maps.<Parameter, Argument>newHashMapWithExpectedSize(this.positional.length + this.named.size());
		var name = new MutableObject<String>(null);
		var argumentBuffer = new StringBuilder(input.length());

		int i = -1;
		while (i != input.length()) {
			int next = input.indexOf(' ', i + 1);
			if (next == -1)
				next = input.length();

			parsePart(input, arguments, positionalIndex, name, argumentBuffer, i, next);

			i = next;
		}

		if (name.getValue() != null)
			throw new UsageException("Argument without value: --" + name);

		if (!argumentBuffer.isEmpty() && !foldPositionalParameter(arguments, positionalIndex, argumentBuffer))
			throw new UsageException("Too many arguments"); // positional argument out of bounds
		return arguments;
	}

	private void parsePart(@Nonnull String input, @Nonnull Map<Parameter, Argument> arguments,
						   @Nonnull MutableInt positionalIndex, @Nonnull MutableObject<String> name,
						   @Nonnull StringBuilder argumentBuffer, int i, int next) {
		if (next - i == 1) {
			if (!argumentBuffer.isEmpty())
				argumentBuffer.append(' ');

		} else {
			var part = input.substring(i + 1, next);

			if (name.getValue() != null) {
				// fold argumentBuffer into named parameter
				var parameter = this.named.get(name.getValue());
				if (parameter == null)
					throw new UsageException("Invalid argument: --" + name);

				arguments.put(parameter, new Argument(part));
				name.setValue(null);

			} else if (part.length() >= 3 && part.charAt(0) == '-' && part.charAt(1) == '-') {
				// start named argument (and fold previous positional argument if one exists)
				if (!argumentBuffer.isEmpty()) {
					if (!foldPositionalParameter(arguments, positionalIndex, argumentBuffer))
						throw new UsageException("Too many arguments");

					positionalIndex.increment();
				}

				name.setValue(part.substring(2).toLowerCase());

			} else if (positionalIndex.getValue() < this.positional.length - 1) {
				// fold single string positional argument
				arguments.put(this.positional[positionalIndex.getAndIncrement()], new Argument(part));

			} else {
				// append to multi string buffer
				if (!argumentBuffer.isEmpty())
					argumentBuffer.append(' ');
				argumentBuffer.append(part);
			}
		}
	}

	public Stream<Parameter> parameters() {
		return concat(stream(this.positional), this.named.values().stream());
	}

	private boolean foldPositionalParameter(@Nonnull Map<Parameter, Argument> arguments, MutableInt positionalIndex,
											@Nonnull StringBuilder argumentBuffer) {
		if (positionalIndex.intValue() >= this.positional.length)
			// this can happen if positional arguments are broken up by a named one, eg.
			// "first --named value second", which yields two positional arguments
			return false;

		arguments.put(this.positional[positionalIndex.getAndIncrement()],
					  new Argument(argumentBuffer.toString().trim()));
		argumentBuffer.setLength(0);
		return true;
	}

	public static class Parameter {

		@Nonnull private final ParameterType type;
		@Nonnull private final String name;
		@Nonnull private final Optional<String> description;
		private final boolean mandatory;

		private Parameter(@Nonnull ParameterType type, @Nonnull String name, @Nonnull Optional<String> description,
						  boolean mandatory) {
			this.type = type;
			this.name = name;
			this.description = description;
			this.mandatory = mandatory;
		}

		@Nonnull
		@SuppressWarnings("null")
		public static MandatoryParameter mandatory(@Nonnull ParameterType type, @Nonnull String name,
												   @Nonnull String description) {
			return new MandatoryParameter(type, name, Optional.of(description));
		}

		@Nonnull
		@SuppressWarnings("null")
		public static MandatoryParameter mandatory(@Nonnull ParameterType type, @Nonnull String name) {
			return new MandatoryParameter(type, name, Optional.empty());
		}

		@Nonnull
		@SuppressWarnings("null")
		public static Parameter optional(@Nonnull ParameterType type, @Nonnull String name,
										 @Nonnull String description) {
			return new Parameter(type, name, Optional.of(description), false);
		}

		@Nonnull
		@SuppressWarnings("null")
		public static Parameter optional(@Nonnull ParameterType type, @Nonnull String name) {
			return new Parameter(type, name, Optional.empty(), false);
		}

		@Nonnull
		public ParameterType getType() {
			return this.type;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}

		@Nonnull
		public Optional<String> getDescription() {
			return this.description;
		}

		public boolean isMandatory() {
			return this.mandatory;
		}

		public enum ParameterType {
			POSITIONAL,
			NAMED
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof Parameter other)
				return Objects.equals(this.name, other.name);
			else
				return false;
		}

		@Override
		public String toString() {
			var out = new StringBuilder(this.name.length() + (this.mandatory ? 0 : 2) + (this.type == NAMED ? 2 : 0));

			if (!this.mandatory)
				out.append('[');

			if (this.type == NAMED)
				out.append("--");

			out.append(this.name);

			if (!this.mandatory)
				out.append(']');

			return out.toString();
		}

	}

	public static class MandatoryParameter extends Parameter {

		MandatoryParameter(@Nonnull ParameterType type, @Nonnull String name, @Nonnull Optional<String> description) {
			super(type, name, description, true);
		}

	}

}
