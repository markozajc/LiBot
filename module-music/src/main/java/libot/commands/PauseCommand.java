package libot.commands;

import static libot.commands.MusicCommandUtils.*;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class PauseCommand extends Command {

	public PauseCommand() {
		super(CommandMetadata.builder(MUSIC, "pause")
			.requireDjRole(true)
			.description("Pauses or resumes audio playback."));
	}

	@Nonnull static final String RESUMED = EMOJI_PLAY + " Playback resumed";
	@Nonnull static final String PAUSED = EMOJI_PAUSE + " Playback paused";

	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var player = getMusicManager(vc).getPlayer();
		if (player.getPlayingTrack() == null)
			throw nothingIsPlaying(c);

		if (player.isPaused())
			c.reply(RESUMED, LITHIUM);
		else
			c.reply(PAUSED, LITHIUM);
		player.setPaused(!player.isPaused());
	}

}
