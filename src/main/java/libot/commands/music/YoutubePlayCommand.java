package libot.commands.music;

import static libot.commands.music.MusicCommandUtils.*;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class YoutubePlayCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var selected = selectTrack(c, youtubeSearch(c.params().get(0)));
		playUrl(c, YOUTUBE_URL_PREFIX + selected.getIdentifier());
	}

	@Override
	public String getName() {
		return "youtubeplay";
	}

	@Override
	public String[] getAliases() {
		return array("ytplay", "youtube", "yt");
	}

	@Override
	public String getInfo() {
		return "Queries Youtube and play a track you pick.";
	}

	@Override
	public String[] getParameters() {
		return array("query");
	}

	@Override
	public String[] getParameterInfo() {
		return array("query to search on youtube");
	}

	@Override
	public void startupCheck(CommandContext c) {
		super.startupCheck(c);
		c.requireDj();
	}

	@Override
	public CommandCategory getCategory() {
		return MUSIC;
	}

}
