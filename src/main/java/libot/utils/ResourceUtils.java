package libot.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.*;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

public final class ResourceUtils {

	private ResourceUtils() {}

	@SuppressWarnings("null")
	@Nonnull
	public static String resourceAsString(@Nonnull String file) throws IOException {
		try (var resourceStream = resourceAsStream(file)) {
			return IOUtils.toString(resourceStream, UTF_8);
		}
	}

	@SuppressWarnings("null")
	@Nonnull
	public static byte[] resourceAsBytes(@Nonnull String file) throws IOException {
		try (var resourceStream = resourceAsStream(file)) {
			return toByteArray(resourceStream);
		}
	}

	@Nonnull
	public static InputStream resourceAsStream(@Nonnull String file) throws FileNotFoundException {
		InputStream stream = ResourceUtils.class.getResourceAsStream("/" + file);
		if (stream == null)
			throw new FileNotFoundException("Couldn't find resource " + file);
		return stream;
	}
}
