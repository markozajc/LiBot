package libot.core.ratelimits;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import libot.core.commands.Command;

public class CommandRatelimitManager {

	private static final Map<String, Ratelimit> RATELIMITS = new ConcurrentHashMap<>();

	@Nonnull
	@SuppressWarnings("null")
	public static Ratelimit getRatelimits(@Nonnull Command command) {
		return RATELIMITS.computeIfAbsent(command.getRatelimitBucket(), k -> new Ratelimit(command.getRatelimit()));
	}

	public static long getRemaining(@Nonnull Command command, long user) {
		return getRatelimits(command).check(user);
	}

	public static boolean isRatelimited(@Nonnull Command command, long user) {
		return getRemaining(command, user) > 0;
	}

	private CommandRatelimitManager() {}

}
