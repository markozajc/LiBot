package libot.utils;

import static java.lang.Math.log10;
import static java.lang.System.getenv;
import static libot.core.Constants.*;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.util.concurrent.*;
import java.util.function.Consumer;

import javax.annotation.*;

public final class Utilities {

	private static final String[] DANGEROUS_ENVIRONMENT =
		{ ENV_SYSADMINS, ENV_RESOURCE_GUILDS, ENV_YOUTUBE_PSID, ENV_YOUTUBE_PAPISID, ENV_GOOGLE_TOKENS };

	@Nonnull
	public static String plural(int n) {
		return n != 1 ? "s" : "";
	}

	@SuppressWarnings("unchecked")
	public static <X extends Throwable> RuntimeException asUnchecked(Throwable ex) throws X {
		throw (X) ex;
	}

	public static int getDigits(int n) {
		return (int) (log10(n) + 1);
	}

	public static <T> T random(T[] elements) {
		return elements[ThreadLocalRandom.current().nextInt(elements.length)];
	}

	@SafeVarargs
	@Nonnull
	public static <T> T[] array(@Nonnull T... elements) {
		return elements;
	}

	@Nonnull
	public static String getenvOrThrow(@Nonnull String key) {
		var value = getenv(key);
		if (value == null)
			throw new IllegalStateException(key + " environment variable is not set");
		return value;
	}

	public static <T> void ifNonNull(@Nullable T value, @Nonnull Consumer<T> consumer) {
		if (value != null)
			consumer.accept(value);
	}

	@Nonnull
	public static <T> CompletableFuture<T> exceptionFuture(@Nonnull Throwable t) {
		var cf = new CompletableFuture<T>();
		cf.obtrudeException(t);
		return cf;
	}

	public static boolean isEnvvarDangerous(@Nonnull String variable) {
		return contains(DANGEROUS_ENVIRONMENT, variable) || variable.startsWith(ENV_SHRED_TOKEN);
	}

	private Utilities() {}

}
