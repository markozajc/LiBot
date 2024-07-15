package libot.commands;

import static libot.commands.MusicCommandUtils.*;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class SkipCommand extends Command {

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		int amount = c.params().getIntOrDefault(0, 1);
		if (amount < 1)
			throw c.error("Can not skip backwards", FAILURE);

		var track = getMusicManager(vc).getScheduler().skipTrack(amount);

		if (track != null)
			c.replyf(FORMAT_PLAY_TRACK, SUCCESS, escape(track.getInfo().title, true), track.getInfo().uri,
					 escape(track.getInfo().author, true));
		else
			c.react(ACCEPT_EMOJI);
	}

	@Override
	public String getName() {
		return "skip";
	}

	@Override
	public String getInfo() {
		return """
			Skips to the next track in queue, stops playing if there are no more queued tracks.""";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "[tracks]" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "number of tracks to skip, 1 by default" };
	}

	@Override
	public int getMinParameters() {
		return 0;
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
