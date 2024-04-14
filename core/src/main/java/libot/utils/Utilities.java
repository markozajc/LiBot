package libot.utils;

import static java.lang.Math.log10;
import static java.lang.System.getenv;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collector;

import javax.annotation.*;

public final class Utilities {

	public static <T, K, U> Collector<T, ?, HashMap<K, U>> toModifiableMap(Function<? super T, ? extends K> keyMapper,
																		   Function<? super T, ? extends U> valueMapper) {
		return toMap(keyMapper, valueMapper, (m1, m2) -> {
			throw new IllegalStateException("Duplicate key");
		}, HashMap::new);
	}

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

	@Nonnull
	public static String getenvOrThrow(@Nonnull String key) {
		var value = getenv(key);
		if (value == null)
			throw new IllegalStateException(key + " environment variable is not set");
		else
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

	private Utilities() {}

}
