package libot.utils;

import static java.lang.reflect.Modifier.*;
import static org.reflections.scanners.Scanners.SubTypes;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.eu.zajc.ef.function.except.EFunction;
import org.reflections.Reflections;
import org.slf4j.Logger;

public class ReflectionUtils {

	private static final Logger LOG = getLogger(ReflectionUtils.class);

	@Nonnull
	public static <T> Stream<T> scanClasspath(Class<? extends T> supertype, Class<?> packageAnchor) {
		return scanClasspath(supertype, packageAnchor, c -> c.getDeclaredConstructor().newInstance());
	}

	@Nonnull
	public static <T> Stream<T> scanClasspath(Class<? extends T> supertype, Class<?> packageAnchor,
											  @Nonnull Class<?>[] argumentTypes, @Nonnull Object... arguments) {
		return scanClasspath(supertype, packageAnchor,
							 c -> c.getDeclaredConstructor(argumentTypes).newInstance(arguments));
	}

	@Nonnull
	@SuppressWarnings({ "unchecked", "null" })
	public static <T> Stream<T> scanClasspath(Class<? extends T> supertype, Class<?> packageAnchor,
											  EFunction<Class<? extends T>, T, ReflectiveOperationException> instantizer) {
		var reflections = new Reflections(packageAnchor.getPackageName());
		return reflections.get(SubTypes.of(supertype).asClass())
			.stream()
			.filter(c -> !isAbstract(c.getModifiers()))
			.filter(c -> !isInterface(c.getModifiers()))
			.map(c -> {
				try {
					return instantizer.applyChecked((Class<? extends T>) c);
				} catch (ReflectiveOperationException e) {
					LOG.error("Failed to instantiate {}:", c.getCanonicalName());
					LOG.error("", e);
					return null;
				}
			})
			.filter(Objects::nonNull);
	}

	private ReflectionUtils() {}

}
