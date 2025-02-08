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
package libot.core;

import static java.lang.System.getenv;
import static java.util.Arrays.stream;
import static libot.core.Constants.*;
import static libot.util.Utilities.getenvOrThrow;

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
