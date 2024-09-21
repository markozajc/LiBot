package libot.core;

import static java.lang.System.getenv;
import static java.util.Arrays.stream;
import static libot.core.Constants.*;
import static libot.utils.Utilities.getenvOrThrow;

import javax.annotation.Nonnull;

public record BotConfiguration(String defaultPrefix, @Nonnull long[] sysadminIds) { // NOSONAR [java:S6218] equals not
																					// used

	@SuppressWarnings("null")
	public static BotConfiguration fromEnvironment() {
		var defaultPrefix = getenvOrThrow(ENV_PREFIX);
		long[] sysadminIds;
		if (getenv(ENV_SYSADMINS) != null)
			sysadminIds = stream(getenv(ENV_SYSADMINS).split(",")).mapToLong(Long::parseLong).toArray();
		else
			sysadminIds = new long[0];
		return new BotConfiguration(defaultPrefix, sysadminIds);
	}

}
