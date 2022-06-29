package libot.commands;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.SEARCH;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.utils.GoogleUtils;

public class GoogleCommand extends Command {

	private static final String FORMAT_NOT_FOUND = """
		Google apparently couldn't answer your question. %s""";
	private static final String FORMAT_NOT_FOUND_NSFW_APPEND = """
		If you're searching for a NSFW topic, please note that results in non-nsfw channels are filtered!""";

	@Override
	public void execute(CommandContext c) {
		var result = GoogleUtils.doSearch(c.params().get(0), !c.isChannelNSFW());
		if (result == null) {
			throw c.error(true, "Google broke. Please try again later.", FAILURE);

		} else if (result.blank()) {
			throw c.errorf(true, FORMAT_NOT_FOUND, DISABLED, !c.isChannelNSFW() ? FORMAT_NOT_FOUND_NSFW_APPEND : "");

		} else {
			c.reply(result.title() + "\n" + result.url());
		}
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
