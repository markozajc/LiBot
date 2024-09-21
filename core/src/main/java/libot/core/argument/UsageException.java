package libot.core.argument;

import javax.annotation.Nonnull;

public class UsageException extends RuntimeException {

	public UsageException(@Nonnull String message) {
		super(message);
	}

}
