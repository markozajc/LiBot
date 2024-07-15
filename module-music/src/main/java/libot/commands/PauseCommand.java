package libot.commands;

import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class PauseCommand extends Command {

	@Nonnull public static final String FORMAT_RESUMED = "\u25B6 Playback resumed";
	@Nonnull public static final String FORMAT_PAUSED = "\u23F8 Playback paused";

	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var player = getMusicManager(vc).getPlayer();
		if (player.getPlayingTrack() == null)
			throw nothingIsPlaying(c);

		if (player.isPaused())
			c.reply(FORMAT_RESUMED, LITHIUM);
		else
			c.reply(FORMAT_PAUSED, LITHIUM);
		player.setPaused(!player.isPaused());
	}

	@Override
	public String getName() {
		return "pause";
	}

	@Override
	public String getInfo() {
		return """
			Pauses/resumes audio playback.""";
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
