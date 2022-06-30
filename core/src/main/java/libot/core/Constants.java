package libot.core;

import static java.lang.System.getenv;
import static java.util.Arrays.stream;
import static libot.utils.ResourceUtils.resourceAsString;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_USER;

import java.awt.Color;
import java.io.*;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.slf4j.*;

import com.google.gson.*;

import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.TA_Grid;
import de.vandermeer.asciithemes.u8.U8_Grids;
import marcono1234.gson.recordadapter.RecordTypeAdapterFactory;
import net.dv8tion.jda.api.exceptions.ErrorHandler;

@SuppressWarnings("null")
public class Constants {

	private static final Logger LOG = LoggerFactory.getLogger(Constants.class);

	// Environment
	public static final String ENV_DATA_TYPE = "DATA_TYPE";
	public static final String ENV_DATA_PATH = "DATA_PATH";
	public static final String ENV_PREFIX = "BOT_PREFIX";
	public static final String ENV_SYSADMINS = "BOT_SYSADMINS";
	public static final String ENV_SHRED_TOKEN = "SHRED_TOKEN_";
	public static final String ENV_GOOGLE_TOKENS = "GOOGLE_TOKENS";
	public static final String ENV_MANAGEMENT_PORT = "MANAGEMENT_PORT";
	public static final String ENV_YOUTUBE_PAPISID = "YOUTUBE_PAPISID3";
	public static final String ENV_YOUTUBE_PSID = "YOUTUBE_PSID3";
	public static final String ENV_RESOURCE_GUILDS = "RESOURCE_GUILDS";
	public static final String ENV_QALCULATE_PATH = "QALCULATE_HELPER_PATH";
	public static final String ENV_QALCULATE_HOME = "QALCULATE_HOME_PATH";
	// Make sure to include any dangerous additions into Utilities.DANGEROUS_ENVIRONMENT!

	// Colors
	public static final Color LITHIUM = new Color(11, 92, 147);
	public static final Color SUCCESS = new Color(59, 165, 93);
	public static final Color WARN = new Color(255, 204, 77);
	public static final Color FAILURE = new Color(237, 66, 69);
	public static final Color DISABLED = new Color(198, 198, 198);

	// Emoji
	public static final String ACCEPT_EMOJI = "\u2705";
	public static final String DENY_EMOJI = "\u274E";
	public static final String FAILURE_EMOJI = "\u274C";
	public static final String MISSING_EMOJI = "\uD83D\uDEAB";

	// Limits
	public static final int MAX_CUSTOM_PREFIX_LENGTH = 50;
	public static final int MAX_GREETER_MESSAGE_LENGTH = 500;

	// Strings
	public static final String EXIT_FOOTER = "Type EXIT to exit.";

	// Common formats
	public static final String FORMAT_ROLE_MISSING = """
		Role "%s" does not exist""";

	// Miscellaneous
	public static final ErrorHandler PRIVATE_MESSAGE_ERROR_HANDLER = new ErrorHandler().ignore(UNKNOWN_USER);
	public static final File PROPERTIES_DIRECTORY = new File("config");
	public static final Gson GSON =
		new GsonBuilder().registerTypeAdapterFactory(RecordTypeAdapterFactory.DEFAULT).create();
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
	@Nonnull
	public static final long[] RESOURCE_GUILDS;
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
