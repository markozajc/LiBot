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
package libot.core;

import static java.lang.System.getenv;
import static java.util.Arrays.stream;
import static libot.utils.ResourceUtils.resourceAsString;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.Color;
import java.io.*;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.gson.Gson;

import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.TA_Grid;
import de.vandermeer.asciithemes.u8.U8_Grids;
import net.dv8tion.jda.api.entities.emoji.Emoji;

@SuppressWarnings("null")
public class Constants {

	private static final Logger LOG = getLogger(Constants.class);

	// Environment
	public static final String ENV_DATA_TYPE = "DATA_TYPE";
	public static final String ENV_DATA_PATH = "DATA_PATH";
	public static final String ENV_PREFIX = "BOT_PREFIX";
	public static final String ENV_SYSADMINS = "BOT_SYSADMINS";
	public static final String ENV_SHRED_TOKEN = "SHRED_TOKEN_";
	public static final String ENV_GOOGLE_TOKENS = "GOOGLE_TOKENS";
	public static final String ENV_GOOGLE_ID = "GOOGLE_ID";
	public static final String ENV_MANAGEMENT_PORT = "MANAGEMENT_PORT";
	public static final String ENV_YOUTUBE_EMAIL = "YOUTUBE_EMAIL";
	public static final String ENV_YOUTUBE_PASSWORD = "YOUTUBE_PASSWORD";
	public static final String ENV_RESOURCE_GUILDS = "RESOURCE_GUILDS";
	public static final String ENV_QALCULATE_PATH = "QALCULATE_HELPER_PATH";
	public static final String ENV_QALCULATE_EXCHANGE_RATE_UPDATER_PATH = "QALCULATE_HELPER_EXCHANGE_RATE_UPDATER_PATH";
	public static final String ENV_QALCULATE_HOME = "QALCULATE_HOME_PATH";

	// Colors
	public static final Color LITHIUM = new Color(11, 92, 147);
	public static final Color SUCCESS = new Color(59, 165, 93);
	public static final Color WARN = new Color(255, 204, 77);
	public static final Color FAILURE = new Color(237, 66, 69);
	public static final Color DISABLED = new Color(198, 198, 198);

	// Emoji
	@Nonnull public static final Emoji ACCEPT_EMOJI = Emoji.fromUnicode("\u2705");
	@Nonnull public static final Emoji DENY_EMOJI = Emoji.fromCustom("decline", 1287493019309834274L, false);
	@Nonnull public static final Emoji FAILURE_EMOJI = Emoji.fromUnicode("\u274C");
	@Nonnull public static final Emoji MISSING_EMOJI = Emoji.fromUnicode("\uD83D\uDEAB");

	// Limits
	public static final int MAX_CUSTOM_PREFIX_LENGTH = 50;
	public static final int MAX_GREETER_MESSAGE_LENGTH = 500;

	// Strings
	public static final String EXIT_FOOTER = "Type EXIT to exit.";

	// Common formats
	public static final String FORMAT_ROLE_MISSING = """
		Role "%s" does not exist""";

	// Miscellaneous
	public static final File PROPERTIES_DIRECTORY = new File("config");
	public static final Gson GSON = new Gson();
	public static final Consumer<Throwable> EMPTY_FAIL_CONSUMER = e -> {};
	public static final String VERSION;
	static {
		String versionString;
		try {
			versionString = resourceAsString("version");
		} catch (IOException e) {
			LOG.warn("Could not read the version", e);
			versionString = "unknown version";
		}
		VERSION = versionString;
	}
	@Nonnull public static final long[] RESOURCE_GUILDS;
	static {
		if (getenv(ENV_RESOURCE_GUILDS) != null) {
			RESOURCE_GUILDS = stream(getenv(ENV_RESOURCE_GUILDS).split(",")).mapToLong(Long::parseLong).toArray(); // NOSONAR
			// no need for security
		} else {
			RESOURCE_GUILDS = new long[0];
		}
	}

	// Tables
	public static final TA_Grid TABLE_GRID = U8_Grids.borderStrongDoubleLight();
	public static final CWC_LongestLine TABLE_CWC = new CWC_LongestLine();

	private Constants() {}

}
