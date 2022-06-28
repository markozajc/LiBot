package libot.core;

import static java.util.Arrays.stream;
import static libot.core.Constants.*;
import static libot.utils.Utilities.getenvOrThrow;

import javax.annotation.Nonnull;

public record BotConfiguration(@Nonnull String defaultPrefix, @Nonnull long[] sysadminIds) { // NOSONAR [java:S6218]
																							 // equals not used

	@SuppressWarnings("null")
	public static BotConfiguration fromEnvironment() {
		var defaultPrefix = getenvOrThrow(ENV_PREFIX);
		long[] sysadminIds = stream(getenvOrThrow(ENV_SYSADMINS).split(",")).mapToLong(Long::parseLong).toArray();
		return new BotConfiguration(defaultPrefix, sysadminIds);
	}

}
