package libot.commands.music;

import static java.lang.Math.round;
import static java.lang.String.format;
import static libot.commands.music.MusicCommandUtils.*;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.core.music.GlobalMusicManager.getMusicManager;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

import javax.annotation.Nonnull;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.core.music.GlobalMusicManager.MusicManager;

public class PlayingCommand extends Command {

	private static final String FORMAT_META = """
		**`Title:   `** [%s](%s)
		**`Author:  `** %s
		**`Duration:`** %s
		```
		%s %s %s```
		""";
	public static final String FORMAT_YT_THUMBNAIL = "http://img.youtube.com/vi/%s/mqdefault.jpg";
	public static final String DEFAULT_THUMBNAIL = "https://libot.eu.org/img/music.png";
	public static final int PROGRESS_BAR_LENGTH = 21;

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getAudioManager().getConnectedChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);
		var track = manager.getPlayingTrack();
		if (track == null)
			throw nothingIsPlaying(c);

		var e = new EmbedPrebuilder(LITHIUM);
		e.setTitle("Information about current song");
		var i = track.getInfo();
		e.setDescriptionf(FORMAT_META, escape(i.title, true), i.uri, escape(i.author, true), duration(track),
						  playerState(manager), progressBar(track), time(track));

		if (i.uri.startsWith(YOUTUBE_URL_PREFIX))
			e.setThumbnail(format(FORMAT_YT_THUMBNAIL, i.identifier));
		else
			e.setThumbnail(DEFAULT_THUMBNAIL);

		c.reply(e);
	}

	private static String playerState(@Nonnull MusicManager manager) {
		if (manager.getPlayer().isPaused())
			return "\u275A\u275A";
		else if (manager.getScheduler().isLoop())
			return "\uD83D\uDD01";
		else
			return "\u25B6";
	}

	@Nonnull
	private static StringBuilder progressBar(@Nonnull AudioTrack track) {
		var progress = new StringBuilder("—".repeat(PROGRESS_BAR_LENGTH));
		if (track.getInfo().isStream)
			progress.append('—');
		else
			progress.insert(round((float) track.getPosition() / (float) track.getDuration() * PROGRESS_BAR_LENGTH),
							'o');
		return progress;
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String time(@Nonnull AudioTrack track) {
		return formatDuration(track.getPosition(), "HH:mm:ss");
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String duration(@Nonnull AudioTrack track) {
		if (track.getInfo().isStream)
			return "LIVE \uD83D\uDD34";
		else
			return formatDuration(track.getDuration(), "HH:mm:ss");
	}

	@Override
	public String getName() {
		return "playing";
	}

	@Override
	public String[] getAliases() {
		return array("p");
	}

	@Override
	public String getInfo() {
		return """
			Displays detailed information about the currently playing track.""";
	}

	@Override
	public CommandCategory getCategory() {
		return MUSIC;
	}

}
