package libot.core.argument;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.*;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.*;
import libot.utils.ParseUtils;

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
