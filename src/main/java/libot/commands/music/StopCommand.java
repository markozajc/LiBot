package libot.commands.music;

import static libot.commands.music.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.SUCCESS;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.core.music.GlobalMusicManager.*;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class StopCommand extends Command {

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
		b.append(".");

		stopPlayback(vc);
		am.closeAudioConnection();

		c.reply("Playback stopped", b.toString(), SUCCESS);
	}

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public String getInfo() {
		return """
			Stops the playback, clears the queue and disconnects LiBot from the voice channel. \
			If music is no longer playing, you can use this command to 'kick' LiBot out of the voice channel.""";
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
