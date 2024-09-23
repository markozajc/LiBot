//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
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
