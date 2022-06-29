package libot.commands;

import static com.google.code.chatterbotapi.ChatterBotType.PANDORABOTS;
import static java.util.regex.Pattern.compile;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.array;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.code.chatterbotapi.*;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class ChatbotCommand extends Command {

	private static final String CHOMSKY_ID = "b0dafd24ee35a477";
	private static final Pattern XML_REGEX = compile("<(.*?)((?= \\/>)|>)");
	private static final Logger LOG = getLogger(ChatbotCommand.class);
	private static final ChatterBot CHATTER_BOT;

	static {
		ChatterBot chatterBot;
		try {
			chatterBot = new ChatterBotFactory().create(PANDORABOTS, CHOMSKY_ID);
		} catch (Exception e) {
			chatterBot = null;
			LOG.error("Failed to load the chatbot", e);
		}

		CHATTER_BOT = chatterBot;
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) throws Exception {
		if (CHATTER_BOT == null)
			throw c.error("This feature is currently not available. Please try again later!", DISABLED);

		var session = CHATTER_BOT.createSession();

		c.reply("You're connected!", """
			You can now start chatting with Chomsky. Say hi!
			Also, if you want Chomsky to ignore a message, prefix it with `>` (eg. `> this message is \
			ignored`).""", "Type in EXIT to quit", SUCCESS);

		while (true) {
			var m = c.askraw();
			if ("exit".equalsIgnoreCase(m.getContentStripped())) {
				m.addReaction(ACCEPT_EMOJI).queue();
				break;
			}

			if (!m.getContentRaw().startsWith(">")) {
				String reply = session.think(m.getContentStripped());
				if (reply.length() == 0)
					c.reply("...");
				else
					c.reply(XML_REGEX.matcher(reply).replaceAll(""));
			}
		}

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
			[Chomsky](http://demo.vhost.pandorabots.com/pandora/talk?botid=b0dafd24ee35a477).""";
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
