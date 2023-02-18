package libot.commands;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.*;
import static libot.commands.GoogleCommand.SearchResult.BLANK_RESULT;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.SEARCH;
import static libot.utils.Utilities.array;
import static org.apache.commons.text.StringEscapeUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.*;

import org.slf4j.Logger;

import com.google.common.cache.Cache;

import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class GoogleCommand extends Command {

	private static final Logger LOG = getLogger(GoogleCommand.class);

	public static record SearchResult(String title, String url, boolean blank) {

		@Nonnull public static final SearchResult BLANK_RESULT = new SearchResult(null, null, true);

	}

	private static record SearchParameters(@Nonnull String query, boolean safeSearch) {}

	private static final Pattern BRACKET_STRINGS = compile("\\<[^\\>]+\\>");
	private static final Pattern MULTIPLE_SPACES = compile("\\s+", UNICODE_CHARACTER_CLASS);

	private static final String API_FORMAT = """
		https://www.googleapis.com/customsearch/v1\
		?cx=018291224751151548851%%3Ajzifriqvl1o\
		&num=1\
		&safe=%s\
		&key=%s\
		&q=%s""";
	private static final String USER_AGENT = """
		Mozilla/5.0 \
		(Windows NT 10.0; Win64; rv:83.1) \
		Gecko/20100101 \
		Firefox/83.1""";

	private static final String FORMAT_NOT_FOUND = """
		Google apparently couldn't answer your question. %s""";
	private static final String FORMAT_NOT_FOUND_NSFW_APPEND = """
		If you're searching for a NSFW topic, please note that results in non-nsfw channels are filtered!""";

	private static final String[] API_KEYS;
	private static final Cache<SearchParameters, SearchResult> CACHE;
	static {
		var keys = getenv(ENV_GOOGLE_TOKENS);
		if (keys == null || keys.isBlank()) {
			LOG.warn("{} is unset or blank. Google-related commands will be unavailable.", ENV_GOOGLE_TOKENS);
			API_KEYS = new String[] {};
			CACHE = null;

		} else {
			API_KEYS = keys.split(",");
			CACHE = newBuilder().maximumSize(API_KEYS.length * 100L).expireAfterWrite(Duration.ofDays(7)).build();
		}
	}

	@Override
	public void execute(CommandContext c) {
		if (API_KEYS.length == 0)
			throw c.error(true, "Google is currently unavailable", DISABLED);

		var result = performSearch(c.params().get(0), !c.isChannelNSFW());
		if (result == null) {
			throw c.error(true, "Google broke. Please try again later.", FAILURE);

		} else if (result.blank()) {
			throw c.errorf(true, FORMAT_NOT_FOUND, DISABLED, !c.isChannelNSFW() ? FORMAT_NOT_FOUND_NSFW_APPEND : "");

		} else {
			c.reply(result.title() + "\n" + result.url());
		}
	}

	@Nullable
	@SuppressWarnings("null")
	private static SearchResult performSearch(@Nonnull String query, boolean safeSearch) {
		var params = new SearchParameters(query.toLowerCase(), safeSearch);
		try {
			return CACHE.get(params, () -> {
				for (var key : API_KEYS) {
					try {
						return performApiSearch(params, key);
					} catch (Exception e) {
						LOG.error("Caught an exception while searching", e);
						continue;
					}
				}

				throw new IOException();
			});
		} catch (ExecutionException e) {
			return null;
		}
	}

	@Nonnull
	private static SearchResult performApiSearch(@Nonnull SearchParameters params,
												 @Nonnull String apiKey) throws IOException {
		var url =
			format(API_FORMAT, params.safeSearch() ? "high" : "off", apiKey, URLEncoder.encode(params.query(), UTF_8));
		var response = Unirest.get(url).headerReplace("User-Agent", USER_AGENT).asJson();
		var json = response.getBody().getObject();

		if (response.isSuccess()) {
			if (json.has("items"))
				return jsonToSearchResult(json.getJSONArray("items").getJSONObject(0));
			else
				return BLANK_RESULT;

		} else {
			throw new IOException(format("Got a %d searching Google: %s", response.getStatus(),
										 json.getJSONArray("errors").getJSONObject(0).getString("message")));
		}
	}

	@Nonnull
	private static SearchResult jsonToSearchResult(JSONObject googleResult) {
		return new SearchResult(cleanString(googleResult.getString("title")),
								URLDecoder.decode(cleanString(googleResult.getString("link")), UTF_8), false);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String cleanString(String uncleanString) {
		var result = uncleanString;
		result = BRACKET_STRINGS.matcher(result).replaceAll("");
		result = MULTIPLE_SPACES.matcher(result).replaceAll(" ");
		result = result.replace("\"", "");
		result = unescapeHtml4(result);
		result = unescapeJava(result);
		return result;
	}

	@Override
	public String getName() {
		return "google";
	}

	@Override
	public String[] getAliases() {
		return array("g");
	}

	@Override
	public String getInfo() {
		return "Searches the provided query on Google. SafeSearch is enabled in non-NSFW channels.";
	}

	@Override
	public String[] getParameters() {
		return array("query");
	}

	@Override
	public int getRatelimit() {
		return 5;
	}

	@Override
	public CommandCategory getCategory() {
		return SEARCH;
	}

}
