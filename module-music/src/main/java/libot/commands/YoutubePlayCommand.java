package libot.commands;

import static libot.commands.MusicCommandUtils.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.MUSIC;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class YoutubePlayCommand extends Command {

	@Nonnull private static final MandatoryParameter QUERY = mandatory(POSITIONAL, "query", "the search query");

	public YoutubePlayCommand() {
		super(CommandMetadata.builder(MUSIC, "youtubeplay")
			.aliases("ytplay", "youtube", "yt")
			.requireDjRole(true)
			.parameters(QUERY)
			.description("""
				Searches for and plays a track from [YouTube](https://youtube.com). If you already know the URL, \
				use the 'play' command"""));
	}

	@SuppressWarnings("null")
	@Override
	public void execute(CommandContext c) {
		var selected = selectTrack(c, youtubeSearch(c.arg(QUERY).value()));
		playUrl(c, YOUTUBE_URL_PREFIX + selected.getIdentifier());
	}

}
