package libot.commands;

import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.SUCCESS;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.*;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class StopCommand extends Command {

	public StopCommand() {
		super(CommandMetadata.builder(MUSIC, "stop").requireDjRole(true).description("""
			Stops the playback, clears the queue and disconnects LiBot from the voice channel. \
			If music is no longer playing, you can use this command to 'kick' LiBot out of the voice channel."""));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(@Nonnull CommandContext c) {
		var am = c.getAudioManager();
		var vc = am.getConnectedChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);

		var b = new StringBuilder("Disconnected from the voice channel");
		if (manager.getPlayingTrack() != null)
			b.append(" & stopped currently playing track");
		if (!manager.getScheduler().isEmpty())
			b.append(" & cleared the queue");
		b.append('.');

		stopPlayback(c.getGuildIdLong());
		am.closeAudioConnection();

		c.reply("Playback stopped", b.toString(), SUCCESS);
	}

}
