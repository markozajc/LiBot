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
package libot.command;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.primitives.Ints.constrainToRange;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.regex.Pattern.compile;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.*;
import static libot.core.command.CommandCategory.SEARCH;
import static net.dv8tion.jda.api.entities.MessageEmbed.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.dv8tion.jda.api.utils.MarkdownUtil.italics;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.google.common.cache.Cache;

import kong.unirest.core.*;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.*;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;

public class UrbanDictionaryCommand extends Command {

	@Nonnull private static final MandatoryParameter TERM = mandatory(POSITIONAL, "term", "term to define");
	@Nonnull private static final Parameter INDEX = optional(NAMED, "index", "which definition to show, 1 by default");

	public UrbanDictionaryCommand() {
		super(CommandMetadata.builder(SEARCH, "urbandictionary")
			.aliases("urban", "ud")
			.parameters(TERM, INDEX)
			.description("Shows a definition from the [Urban Dictionary](https://www.urbandictionary.com/)."));
	}

	private static final Cache<String, String> TERMS_CACHE =
		newBuilder().expireAfterWrite(7, DAYS).maximumSize(4096).build();

	private static record Definition(String word, String definition, String example, String author, int thumbs_up,
		int thumbs_down, int index, int count) {}

	private static final UnirestInstance NO_REDIRECT_UNIREST = Unirest.spawnInstance();
	static {
		NO_REDIRECT_UNIREST.config().followRedirects(false);
	}

	private static final Pattern REFERENCE_PATTERN = compile("\\[(.*?)\\]");
	private static final String ENDPOINT_API = "https://api.urbandictionary.com/v0/define";
	private static final String ENDPOINT_WEB = "https://www.urbandictionary.com/define.php?term=%s";

	private static final String FORMAT_REFERENCE = "[%%s](%s)".formatted(ENDPOINT_WEB);

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) throws Exception {
		try {
			var term = TERMS_CACHE.get(c.arg(TERM).value(), () -> getTerm(c.arg(TERM)));

			var def = getDefinition(c, term);

			var b = new EmbedPrebuilder(LITHIUM);
			b.setAuthor("A definition by " + def.author());
			b.setTitle("Definition of \"%s\":".formatted(def.word()), ENDPOINT_WEB.formatted(term));
			b.setDescription(italics(def.definition()));
			b.addField("Usage example", def.example(), true);
			b.addField("Votes", """
				\uD83D\uDC4D\uD83C\uDFFC %d
				\uD83D\uDC4E\uD83C\uDFFC %d""".formatted(def.thumbs_up(), def.thumbs_down()), true);

			String hint = "";
			if (c.arg(INDEX).isEmpty()) {
				hint = "\nShow another definition with %s %s --index 2".formatted(c.getCommandWithPrefix(),
																				  c.arg(TERM).value());
			}

			b.setFooterf("Definition %d out of %d" + hint, def.index(), def.count());

			c.reply(b);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof Exception ex)
				throw ex;
			else
				throw e;
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String getTerm(@Nonnull Argument arg) {
		String term = URLEncoder.encode(arg.value(), UTF_8);
		var header = NO_REDIRECT_UNIREST.get(ENDPOINT_WEB.formatted(term)).asEmpty().getHeaders().getFirst("Location");
		if (!header.isEmpty())
			return header.substring(header.indexOf('=') + 1);
		else
			return term;
		// The website redirects certain terms (eg. lol -> LOL), which is not signaled in the
		// API, so we must make a request to the website and check for a HTTP redirect
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Definition getDefinition(@Nonnull CommandContext c, @Nonnull String term) {
		var response =
			Unirest.get(ENDPOINT_API).queryString("term", term).asJson().getBody().getObject().getJSONArray("list");

		if (response.isEmpty())
			throw c.errorf("Looks like UrbanDictionary can't define '%s'.", DISABLED, escape(c.arg(TERM).value()));

		// TODO Math.clamp (when java 21)
		int index = constrainToRange(c.arg(INDEX).map(Argument::valueAsInt).orElse(1), 1, response.length());
		var json = response.getJSONObject(index - 1);

		var word = escape(json.getString("word"));
		var definition = cleanText(escape(json.getString("definition"), true), DESCRIPTION_MAX_LENGTH - 4);
		var example = cleanText(escape(json.getString("example")), VALUE_MAX_LENGTH);

		return new Definition(word, definition, example, json.getString("author"), json.getInt("thumbs_up"),
							  json.getInt("thumbs_down"), index, response.length());
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String cleanText(@Nonnull String text, int maxLength) {
		return abbreviate(parseReferences(text.replace("\r", "")), maxLength);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseReferences(@Nonnull String text) {
		return REFERENCE_PATTERN.matcher(text)
			.replaceAll(m -> FORMAT_REFERENCE.formatted(m.group(1), URLEncoder.encode(m.group(1), UTF_8)));
	}

}
