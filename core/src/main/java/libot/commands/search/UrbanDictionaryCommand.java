package libot.commands.search;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.sort;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.regex.Pattern.compile;
import static kong.unirest.Unirest.spawnInstance;
import static libot.commands.search.UrbanDictionaryCommand.Definition.BLANK;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.SEARCH;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.entities.MessageEmbed.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.*;

import org.apache.commons.math3.stat.interval.WilsonScoreInterval;

import com.google.common.cache.Cache;

import kong.unirest.*;
import libot.core.commands.*;
import libot.core.entities.*;
import libot.core.extensions.EmbedPrebuilder;

public class UrbanDictionaryCommand extends Command {

	private static final WilsonScoreInterval WILSON = new WilsonScoreInterval();

	static record Definition(String definition, int thumbs_up, String author, String word, String example,
							 int thumbs_down, boolean empty) {

		public static final Definition BLANK = new Definition(null, 0, null, null, null, 0, true);

		@SuppressWarnings("null")
		public String definition() {
			return abbreviate(parseReferences(this.definition), DESCRIPTION_MAX_LENGTH - 4);
		}

		@SuppressWarnings("null")
		public String example() {
			return abbreviate(parseReferences(this.example), VALUE_MAX_LENGTH);
		}

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
			if (def.empty())
				throw c.errorf("Looks like UrbanDictionary can't define '%s'.", DISABLED, escape(c.params().get(0)));
			var b =
				new EmbedPrebuilder(LITHIUM).addFieldf(true, "Votes", FORMAT_VOTES, def.thumbs_up(), def.thumbs_down())
					.setAuthorf(FORMAT_AUTHOR, def.author())
					.setDescriptionf(FORMAT_DESCRIPTION, def.definition())
					.setTitle(format(FORMAT_TITLE, def.word()),
							  format(WEBSITE_URL, URLEncoder.encode(c.params().get(0), UTF_8)))
					.addField("Usage example", def.example(), true);

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
	private static Definition getDefinition(@Nonnull String term) {
		var response = Unirest.get(format(API_URL, term)).asJson().getBody().getObject().getJSONArray("list");

		if (response.isEmpty())
			return BLANK;

		var definitions = new Definition[response.length()];
		for (int i = 0; i < response.length(); i++) {
			var json = response.getJSONObject(i);
			definitions[i] =
				new Definition(json.getString("definition"), json.getInt("thumbs_up"), json.getString("author"),
							   json.getString("word"), json.getString("example"), json.getInt("thumbs_down"), false);
		}

		String finalTerm = term; // lambda shenanigans
		sort(definitions, (d1, d2) -> {
			boolean d1Match = finalTerm.equals(d1.word());
			boolean d2Match = finalTerm.equals(d2.word());
			if (!d1Match && d2Match)
				return 1;
			else if (d1Match && !d2Match || d2.thumbs_down() == 0 && d1.thumbs_down() != 0)
				return -1;
			else if (d2.thumbs_down() != 0 && d1.thumbs_down() == 0)
				return 1;

			var wilson1 = WILSON.createInterval(d1.thumbs_up() + d1.thumbs_down(), d1.thumbs_up(), .95F);
			var wilson2 = WILSON.createInterval(d2.thumbs_up() + d2.thumbs_down(), d2.thumbs_up(), .95F);

			double score1 = (wilson1.getUpperBound() + wilson1.getLowerBound()) / 2;
			double score2 = (wilson2.getUpperBound() + wilson2.getLowerBound()) / 2;

			return Double.compare(score2, score1);
		});
		return definitions[0];
	}

	@Override
	public String getName() {
		return "urbandictionary";
	}

	@Override
	public String[] getAliases() {
		return array("urban", "ud");
	}

	@Override
	public String getInfo() {
		return "Browses definitions from the [Urban Dictionary](https://www.urbandictionary.com/).";
	}

	@Override
	public String[] getParameters() {
		return array("term");
	}

	@Override
	public String[] getParameterInfo() {
		return array("term to define");
	}

	@Override
	public CommandCategory getCategory() {
		return SEARCH;
	}

}
