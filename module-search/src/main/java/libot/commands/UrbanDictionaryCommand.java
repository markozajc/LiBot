package libot.commands;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.regex.Pattern.compile;
import static kong.unirest.Unirest.spawnInstance;
import static libot.commands.UrbanDictionaryCommand.Definition.BLANK;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.SEARCH;
import static net.dv8tion.jda.api.entities.MessageEmbed.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.*;

import com.google.common.cache.Cache;

import kong.unirest.*;
import libot.core.commands.*;
import libot.core.entities.*;
import libot.core.extensions.EmbedPrebuilder;

public class UrbanDictionaryCommand extends Command {

	static record Definition(String word, String definition, String example, String author, int thumbs_up,
							 int thumbs_down) {

		public static final Definition BLANK = new Definition(null, null, null, null, 0, 0);

	}

	private static final Cache<String, Definition> DEFINITION_CACHE =
		newBuilder().expireAfterWrite(1, DAYS).maximumSize(2048).build();
	private static final Cache<String, String> TERMS_CACHE =
		newBuilder().expireAfterWrite(7, DAYS).maximumSize(4096).build();

	private static final UnirestInstance NO_REDIRECT_UNIREST = spawnInstance();
	static {
		NO_REDIRECT_UNIREST.config().followRedirects(false);
	}

	private static final Pattern REFERENCE_PATTERN = compile("\\[(.*?)\\]");
	private static final String API_URL = "https://api.urbandictionary.com/v0/define?term=%s";
	private static final String WEBSITE_URL = "https://www.urbandictionary.com/define.php?term=%s";

	private static final String FORMAT_DESCRIPTION = """
		_"%s"_""";
	private static final String FORMAT_AUTHOR = """
		A definition by %s""";
	private static final String FORMAT_TITLE = """
		Definition of "%s":""";
	private static final String FORMAT_VOTES = """
		\uD83D\uDC4D\uD83C\uDFFC %d
		\uD83D\uDC4E\uD83C\uDFFC %d""";
	private static final String FORMAT_REFERENCE = format("[%%s](%s)", WEBSITE_URL);

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) throws Exception {
		try {
			var term = TERMS_CACHE.get(c.params().get(0).toLowerCase(), () -> getTerm(c.params()));
			var def = DEFINITION_CACHE.get(term, () -> getDefinition(term));

			if (def.definition() == null)
				throw c.errorf("Looks like UrbanDictionary can't define '%s'.", DISABLED, escape(c.params().get(0)));

			var b = new EmbedPrebuilder(LITHIUM).setAuthorf(FORMAT_AUTHOR, def.author())
				.setDescriptionf(FORMAT_DESCRIPTION, def.definition())
				.setTitle(format(FORMAT_TITLE, def.word()),
						  format(WEBSITE_URL, URLEncoder.encode(c.params().get(0), UTF_8)))
				.addField("Usage example", def.example(), true)
				.addField("Votes", FORMAT_VOTES.formatted(def.thumbs_up(), def.thumbs_down()), true);

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
	private static String getTerm(@Nonnull Parameters p) {
		String term = URLEncoder.encode(p.get(0), UTF_8);
		var header = NO_REDIRECT_UNIREST.get(format(WEBSITE_URL, term)).asEmpty().getHeaders().getFirst("Location");
		if (!header.isEmpty())
			return header.substring(header.indexOf('=') + 1);
		else
			return term;
		// The website redirects certain terms (eg. lol -> LOL), which is not signaled in the
		// API, so we must make a request to the website and check for a HTTP redirect
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseReferences(@Nonnull String text) {
		return REFERENCE_PATTERN.matcher(text)
			.replaceAll(m -> format(FORMAT_REFERENCE, m.group(1), URLEncoder.encode(m.group(1), UTF_8)));
	}

	@Nullable
	@SuppressWarnings("null")
	private static Definition getDefinition(@Nonnull String term) {
		var response = Unirest.get(format(API_URL, term)).asJson().getBody().getObject().getJSONArray("list");

		if (response.isEmpty())
			return BLANK;

		var json = response.getJSONObject(0);

		var word = escape(json.getString("word"));
		var definition = cleanText(escape(json.getString("definition"), true), DESCRIPTION_MAX_LENGTH - 4);
		var example = cleanText(escape(json.getString("example")), VALUE_MAX_LENGTH);

		return new Definition(word, definition, example, json.getString("author"), json.getInt("thumbs_up"),
							  json.getInt("thumbs_down"));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String cleanText(@Nonnull String text, int maxLength) {
		return abbreviate(parseReferences(text.replace("\r", "")), maxLength);
	}

	@Override
	public String getName() {
		return "urbandictionary";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "urban", "ud" };
	}

	@Override
	public String getInfo() {
		return "Browses definitions from the [Urban Dictionary](https://www.urbandictionary.com/).";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "term" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "term to define" };
	}

	@Override
	public CommandCategory getCategory() {
		return SEARCH;
	}

}
