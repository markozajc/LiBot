package libot.commands;

import static com.github.markozajc.ef.EHandle.handle;
import static java.util.regex.Pattern.compile;
import static javax.xml.xpath.XPathConstants.STRING;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.array;
import static org.apache.commons.lang3.tuple.Pair.of;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.*;

import javax.annotation.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;

import kong.unirest.Unirest;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

@SuppressWarnings("java:S4248") // false positive spam (non-static pattern)
public class ChatbotCommand extends Command {

	private static final String PANDORABOTS_EMOJI = "<:pandorabots:1129815071556653067>";
	// nbsp used in place of spaces on the following line because ecj doesn't like
	// codepoints in multiline strings
	// https://github.com/eclipse-jdt/eclipse.jdt.core/issues/1237
	private static final String NAME = "**Chomsky [Chatbot %s]**".formatted(PANDORABOTS_EMOJI); // NOSONAR it stays
	private static final String BOT_ID = "b0dafd24ee35a477";

	private static final String LEARN_TEXT = "Would you like to teach me a new question and answer?";
	private static final String LEARN_RESPONSE = "I'm sorry, but learning is disabled in this session";
	private static final String LEARN_SUGGESTION = "If you would like to teach me a better reply, just say \"Learn\".";

	private static final DocumentBuilder DOCUMENT_BUILDER; // implementation isn't thread safe
	private static final XPathExpression XPATH_EXTRACTOR; // explicitly not thread safe

	private static final List<Pair<Pattern, String>> RESPONSE_PARSERS_RECURSIVE = new ArrayList<>();
	private static final List<Pair<Pattern, String>> RESPONSE_PARSERS = new ArrayList<>();

	static {
		var dbf = DocumentBuilderFactory.newInstance();
		DOCUMENT_BUILDER = handle((Callable<DocumentBuilder>) dbf::newDocumentBuilder, e -> null).get();

		var xp = XPathFactory.newInstance().newXPath();
		XPATH_EXTRACTOR = handle((Callable<XPathExpression>) () -> xp.compile("//result/that/text()"), e -> null).get();

		// useless data, inner data discarded
		RESPONSE_PARSERS.add(of(compile("(?s)<object[^>]*>.*?</object>"), "")); // NOSONAR
		RESPONSE_PARSERS.add(of(compile("(?s)<param[^>]*>.*?</param>"), ""));
		RESPONSE_PARSERS.add(of(compile("(?s)<embed[^>]*>.*?</embed>"), ""));
		RESPONSE_PARSERS.add(of(compile("(?s)<script[^>]*>.*?</script>"), ""));
		RESPONSE_PARSERS.add(of(compile("(?s)<style[^>]*>.*?</style>"), ""));

		// useless tags, inner data kept
		RESPONSE_PARSERS_RECURSIVE.add(of(compile("(?s)<font[^>]*>(.*?)</font>"), "$1"));
		RESPONSE_PARSERS_RECURSIVE.add(of(compile("(?s)<div[^>]*>(.*?)</div>"), "$1\n"));

		// html we can't turn into markdown but we still change
		RESPONSE_PARSERS.add(of(compile("<a href=\"([^\"]*)\"[^>]*>(.*?)<\\/a>"), "$2 (<$1>)"));
		RESPONSE_PARSERS.add(of(compile("<br> *"), "\n"));

		// html we can turn into markdown
		RESPONSE_PARSERS_RECURSIVE.add(of(compile("<b>(.*?)</b>"), "**$1**"));
		RESPONSE_PARSERS_RECURSIVE.add(of(compile("<i>(.*?)</i>"), "*$1*"));
		RESPONSE_PARSERS_RECURSIVE.add(of(compile("<u>(.*?)</u>"), "__$1__"));

		// botched punctuation
		RESPONSE_PARSERS.add(of(compile(" {2,}"), " "));
		RESPONSE_PARSERS.add(of(compile(" +(?=[\\.,!?])"), ""));

		// STATIC_RESPONSES.put(q -> q.startsWith("web search"), )
	}

	@Override
	public void execute(CommandContext c) throws Exception {
		var session = generateSession();

		c.replyf("You're connected!", """
			You can now start chatting with %s. Say hi!
			Also, if you want Chomsky to ignore a message, prefix it with `=` (eg. `= this message is \
			ignored`).""", "Type in EXIT to quit", SUCCESS, NAME);

		while (true) {
			var m = c.askraw();
			if ("exit".equalsIgnoreCase(m.getContentStripped())) {
				m.addReaction(ACCEPT_EMOJI).queue();
				break;

			} else if (!m.getContentRaw().startsWith("=")) {
				c.replyf("%s: %s", NAME, think(m.getContentStripped(), session));
			}
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String generateSession() {
		return UUID.randomUUID().toString();
	}

	@Nonnull
	public static String think(@Nonnull String text, @Nonnull String session) throws XPathExpressionException,
																			  SAXException, IOException {
		if (text.equals("learn") || text.startsWith("learn ")) // return the replacement response
			return LEARN_RESPONSE;

		var response = getResponse(text, session);
		if (response == null) {
			return "...";

		} else if (response.equals(LEARN_TEXT)) {
			getResponse("no", session); // cancel the prompt and return the replacement response
			return LEARN_RESPONSE;

		} else if (response.equals(LEARN_SUGGESTION)) {
			return think("ok", session); // change the query
		}

		response = parseResponse(response);
		if (response.isEmpty())
			return "...";
		else
			return response;
	}

	@Nullable
	@SuppressWarnings("null")
	private static String getResponse(@Nonnull String text, @Nonnull String session) throws SAXException, IOException,
																					 XPathExpressionException {
		var bytes = Unirest.post("https://www.pandorabots.com/pandora/talk-xml")
			.field("botid", BOT_ID)
			.field("custid", session)
			.field("input", text)
			.asBytes()
			.mapBody(ByteArrayInputStream::new);

		return extractResponse(bytes);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseResponse(@Nonnull String response) {
		var parsedResponse = response;
		for (var parser : RESPONSE_PARSERS_RECURSIVE) {
			Matcher m;
			while ((m = parser.getKey().matcher(parsedResponse)).find()) // recursive replaceAll
				parsedResponse = m.replaceAll(parser.getValue());
		}

		for (var parser : RESPONSE_PARSERS)
			parsedResponse = parser.getKey().matcher(parsedResponse).replaceAll(parser.getValue());

		return StringUtils.capitalize(parsedResponse.trim());
	}

	@Nullable
	private static synchronized String extractResponse(@Nonnull ByteArrayInputStream response) throws SAXException,
																							   IOException,
																							   XPathExpressionException {
		var doc = DOCUMENT_BUILDER.parse(response);
		return (String) XPATH_EXTRACTOR.evaluate(doc, STRING);
	}

	@Override
	public String getName() {
		return "chatbot";
	}

	@Override
	public String[] getAliases() {
		return array("chat", "talk");
	}

	@Override
	public String getInfo() {
		return """
			Opens a chat session with \
			[Chomsky](http://demo.vhost.pandorabots.com/pandora/talk?botid=b0dafd24ee35a477), the online chatbot.""";
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
