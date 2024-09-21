package libot.core.argument;

import javax.annotation.Nonnull;

public class ArgumentParseException extends RuntimeException {

	public ArgumentParseException(@Nonnull String message) {
		super(message);
	}

}
