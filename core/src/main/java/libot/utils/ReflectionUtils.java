package libot.utils;

import static java.lang.reflect.Modifier.isAbstract;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.reflections.scanners.Scanners.SubTypes;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.*;

import javax.annotation.Nonnull;

import org.reflections.Reflections;
import org.slf4j.Logger;

import com.github.markozajc.functions.exceptionable.EFunction;

public class ReflectionUtils {

	private static final Logger LOG = getLogger(ReflectionUtils.class);

	@Nonnull
	public static <T> Set<T> scanClasspath(Class<? extends T> supertype, Class<?> packageAnchor) {
		return scanClasspath(supertype, packageAnchor, c -> c.getDeclaredConstructor().newInstance());
	}

	@Nonnull
	public static <T> Set<T> scanClasspath(Class<? extends T> supertype, Class<?> packageAnchor,
										   @Nonnull Class<?>[] argumentTypes, @Nonnull Object... arguments) {
		return scanClasspath(supertype, packageAnchor,
							 c -> c.getDeclaredConstructor(argumentTypes).newInstance(arguments));
	}

	@Nonnull
	@SuppressWarnings({ "unchecked", "null" })
	public static <T> Set<T> scanClasspath(Class<? extends T> supertype, Class<?> packageAnchor,
										   EFunction<Class<? extends T>, T, ReflectiveOperationException> instantizer) {
		var reflections = new Reflections(packageAnchor.getPackageName());
		return reflections.get(SubTypes.of(supertype).asClass())
			.stream()
			.filter(c -> !isAbstract(c.getModifiers()))
			.map(c -> {
				try {
					return instantizer.applyChecked((Class<? extends T>) c);
				} catch (ReflectiveOperationException e) {
					LOG.error("Failed to instantiate {}:", c.getCanonicalName());
					LOG.error("", e);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(toUnmodifiableSet());
	}

	private ReflectionUtils() {}

}
