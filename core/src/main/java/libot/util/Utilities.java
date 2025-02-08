//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.util;

import static java.lang.Math.log10;
import static java.lang.System.getenv;
import static java.util.stream.Collectors.toMap;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.random.RandomGenerator;
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

	@SuppressWarnings("null")
	public static <T> T random(@Nonnull T[] elements) {
		return random(elements, ThreadLocalRandom.current());
	}

	public static <T> T random(@Nonnull T[] elements, @Nonnull RandomGenerator random) {
		return elements[random.nextInt(elements.length)];
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

	@Nonnull
	public static <T> Collection<T> concat(@Nullable T element, @Nonnull T[] more) {
		var result = new ArrayList<T>(1 + more.length);
		result.add(element);
		Collections.addAll(result, more);
		return result;
	}

	private Utilities() {}

}
