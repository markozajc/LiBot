package libot.core.argument;

import static com.google.common.collect.Streams.concat;
import static java.lang.Math.max;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static libot.core.argument.ParameterList.Parameter.ParameterType.NAMED;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.*;

import org.apache.commons.lang3.mutable.*;

import com.google.common.collect.Maps;

import libot.core.argument.ArgumentList.Argument;

public class ParameterList {

	@SuppressWarnings("null")
	@Nonnull static final ParameterList EMPTY = new ParameterList(new Parameter[0], emptyMap(), 0, new Parameter[0]);

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
		if (parameters.length == 0)
			return EMPTY;

		var named = Maps.<String, Parameter>newHashMapWithExpectedSize(parameters.length);
		var namedRequired = new ArrayList<Parameter>(parameters.length);
		var positional = new ArrayList<Parameter>(parameters.length);
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

		int positionalRequired = 0;
		for (int i = positional.size(); i-- > 0;) {
			if (positional.get(i).isMandatory()) {
				positionalRequired = i;
				break;
			}
		}

		return new ParameterList(positional.toArray(Parameter[]::new), new HashMap<>(named), positionalRequired,
								 namedRequired.toArray(Parameter[]::new));
	}

	@Nonnull
	@SuppressWarnings("null")
	public ArgumentList parse(@Nonnull String input) {
		if (input.length() == 0)
			return ArgumentList.EMPTY;

		if (this == EMPTY)
			throw new ArgumentParseException("Too many arguments");

		var arguments =
			Maps.<Parameter, Argument>newHashMapWithExpectedSize(this.positional.length + this.named.size());

		var positionalIndex = new MutableInt(0);
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
			throw new ArgumentParseException("Argument without value: --" + name);

		if (!argumentBuffer.isEmpty() && !foldPositionalParameter(arguments, positionalIndex, argumentBuffer))
			throw new ArgumentParseException("Too many arguments"); // positional argument out of bounds

		if (max(0, positionalIndex.intValue() - 1) < this.positionalRequiredIndex) {
			throw new ArgumentParseException("Missing argument: " +
				this.positional[this.positionalRequiredIndex].getName());
		}

		for (var required : this.namedRequired) {
			if (!arguments.containsKey(required))
				throw new ArgumentParseException("Missing argument: --" + required.getName());
		}

		return new ArgumentList(arguments);
	}

	@SuppressWarnings("null")
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
					throw new ArgumentParseException("Invalid argument: --" + name);

				arguments.put(parameter, new Argument(part));
				name.setValue(null);

			} else if (part.length() >= 3 && part.charAt(0) == '-' && part.charAt(1) == '-') {
				// start named argument (and fold previous positional argument if one exists)
				if (!argumentBuffer.isEmpty()) {
					if (!foldPositionalParameter(arguments, positionalIndex, argumentBuffer))
						throw new ArgumentParseException("Too many arguments");

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

	@SuppressWarnings("null")
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
		@Nonnull private final String description;
		@Nullable private final String defaultValue;
		private final boolean mandatory;

		private Parameter(@Nonnull ParameterType type, @Nonnull String name, @Nonnull String description,
						  @Nullable String defaultValue, boolean mandatory) {
			this.type = type;
			this.name = name;
			this.description = description;
			this.defaultValue = defaultValue;
			this.mandatory = mandatory;
		}

		@Nonnull
		public static MandatoryParameter mandatory(@Nonnull ParameterType type, @Nonnull String name,
												   @Nonnull String description) {
			return new MandatoryParameter(type, name, description, null);
		}

		@Nonnull
		public static Parameter optional(@Nonnull ParameterType type, @Nonnull String name, @Nonnull String description,
										 @Nullable String defaultValue) {
			return new Parameter(type, name, description, defaultValue, false);
		}

		@Nonnull
		public static Parameter optional(@Nonnull ParameterType type, @Nonnull String name,
										 @Nonnull String description) {
			return optional(type, name, description, null);
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
		public String getDescription() {
			return this.description;
		}

		@Nullable
		public String getDefaultValue() {
			return this.defaultValue;
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

		MandatoryParameter(@Nonnull ParameterType type, @Nonnull String name, @Nonnull String description,
						   @Nullable String defaultValue) {
			super(type, name, description, defaultValue, true);
		}

	}

}
