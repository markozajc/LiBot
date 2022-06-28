package libot.core.ratelimits;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import libot.core.commands.Command;

public class RatelimitsManager {

	private static final Map<String, Ratelimits> RATELIMITS = new ConcurrentHashMap<>();

	@Nonnull
	@SuppressWarnings("null")
	public static Ratelimits getRatelimits(@Nonnull Command command) {
		return RATELIMITS.computeIfAbsent(command.getRatelimitId(),
										  k -> new Ratelimits(MILLISECONDS.toSeconds(command.getRatelimit())));
	}

	public static long getRemaining(@Nonnull Command command, long user) {
		return getRatelimits(command).check(user);
	}

	public static boolean isRatelimited(@Nonnull Command command, long user) {
		return getRemaining(command, user) > 0;
	}

	private RatelimitsManager() {}

}
