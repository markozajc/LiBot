package libot.utils;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.*;
import static libot.core.Constants.ENV_GOOGLE_TOKENS;
import static libot.utils.GoogleUtils.SearchResult.BLANK_RESULT;
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

public class GoogleUtils {

	private static final Pattern BRACKET_STRINGS = compile("\\<[^\\>]+\\>");
	private static final Pattern MULTIPLE_SPACES = compile("\\s+", UNICODE_CHARACTER_CLASS);

	public static record SearchResult(String title, String url, boolean blank) {

		@Nonnull
		public static final SearchResult BLANK_RESULT = new SearchResult(null, null, true);

	}

	private static record SearchParameters(@Nonnull String query, boolean safeSearch) {}

	private static final Logger LOG = getLogger(GoogleUtils.class);

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

	private static final String[] API_KEYS;
	private static final Cache<SearchParameters, SearchResult> CACHE;
	static {
		var tokens = getenv(ENV_GOOGLE_TOKENS);
		if (tokens == null) {
			LOG.error("No tokens specified. Google-related commands will be unavailable.");
			API_KEYS = new String[] {};
		} else {
			API_KEYS = tokens.split(",");
		}
		CACHE = newBuilder().maximumSize(API_KEYS.length * 100L).expireAfterWrite(Duration.ofDays(7)).build();
	}

	@Nullable
	@SuppressWarnings("null")
	public static SearchResult doSearch(String query, boolean safeSearch) {
		var params = new SearchParameters(query.toLowerCase(), safeSearch);
		try {
			return CACHE.get(params, () -> {
				for (var key : API_KEYS) {
					try {
						return searchWithApi(params, key);
					} catch (IOException e) {
						LOG.debug("", e);
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
	public static SearchResult searchWithApi(@Nonnull SearchParameters params,
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

}
