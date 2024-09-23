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
package libot.command;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.*;
import static libot.command.GoogleCommand.SearchResult.BLANK_RESULT;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.SEARCH;
import static org.apache.commons.text.StringEscapeUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.*;

import org.slf4j.Logger;

import com.google.common.cache.Cache;

import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;

public class GoogleCommand extends Command {

	@Nonnull private static final MandatoryParameter QUERY = mandatory(POSITIONAL, "query");

	public GoogleCommand() {
		super(CommandMetadata.builder(SEARCH, "google")
			.aliases("g")
			.ratelimit(5, SECONDS)
			.parameters(QUERY)
			.description("Searches the provided query on Google. SafeSearch is enabled in non-NSFW channels."));
	}

	private static final Logger LOG = getLogger(GoogleCommand.class);

	public static record SearchResult(String title, String url, boolean blank) {

		@Nonnull public static final SearchResult BLANK_RESULT = new SearchResult(null, null, true);

	}

	private static record SearchParameters(@Nonnull String query, boolean safeSearch) {}

	private static final Pattern BRACKET_STRINGS = compile("\\<[^\\>]+\\>");
	private static final Pattern MULTIPLE_SPACES = compile("\\s+", UNICODE_CHARACTER_CLASS);

	private static final String API_ENDPOINT = "https://www.googleapis.com/customsearch/v1";
	private static final String ID;

	private static final String[] API_KEYS;
	private static final Cache<SearchParameters, SearchResult> CACHE;
	static {
		var keys = getenv(ENV_GOOGLE_TOKENS);
		var id = getenv(ENV_GOOGLE_ID);
		if (keys == null || keys.isBlank() || id == null || id.isBlank()) {
			LOG.warn("{} and/or {} is unset. Google-related commands will be unavailable.", ENV_GOOGLE_TOKENS,
					 ENV_GOOGLE_ID);
			API_KEYS = new String[] {};
			CACHE = null;
			ID = null;

		} else {
			API_KEYS = keys.split(",");
			CACHE = newBuilder().maximumSize(API_KEYS.length * 100L).expireAfterWrite(Duration.ofDays(7)).build();
			ID = id;
		}
	}

	@SuppressWarnings("null")
	@Override
	public void execute(CommandContext c) {
		if (API_KEYS.length == 0)
			throw c.error(true, "Google is currently unavailable", DISABLED);

		var result = performSearch(c.arg(QUERY).value(), !c.isChannelNSFW());
		if (result == null) {
			throw c.error(true, "Google broke. Please try again later.", FAILURE);

		} else if (result.blank()) {
			throw c.errorf(true, "No results found. %s", DISABLED, !c.isChannelNSFW()
				? "If you're searching for a NSFW topic, please note that results in non-nsfw channels are filtered!"
				: "");

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
		var response = Unirest.get(API_ENDPOINT)
			.queryString("cx", ID)
			.queryString("num", "1")
			.queryString("safe", params.safeSearch() ? "high" : "off")
			.queryString("key", apiKey)
			.queryString("q", params.query())
			.asJson();
		var json = response.getBody().getObject();

		if (response.isSuccess()) {
			if (json.has("items"))
				return jsonToSearchResult(json.getJSONArray("items").getJSONObject(0));
			else
				return BLANK_RESULT;

		} else {
			throw new IOException("Got a %d searching Google: %s"
				.formatted(response.getStatus(),
						   json.getJSONObject("error").getJSONArray("errors").getJSONObject(0).getString("message")));
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
