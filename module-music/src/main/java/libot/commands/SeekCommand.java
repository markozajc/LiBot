package libot.commands;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.*;
import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static libot.utils.ParseUtils.parseRelativeTime;
import static libot.utils.Utilities.array;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggContainerProbe;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import libot.core.commands.*;
import libot.core.entities.*;

public class SeekCommand extends Command {

	private static final TimeUnit[] ABSOLUTE_UNITS = new TimeUnit[] { HOURS, MINUTES, SECONDS };
	private static final String FORMAT_POSITION_FORMAT = """
		The position must be specified in a relative or absolute format, for example:
		Relative:
		 `+1s    ` seek forward one second
		 `-10m   ` seek backwards ten minutes
		 `+1h    ` seek forward one hour
		Absolute:
		 `1:00:00` seek to the first hour
		 `1:00   ` seek to the first minute
		 `1 0    ` ditto
		 `0      ` seek to the beginning
		 `1      ` seek to the first second
		""";
	private static final String OGG_NOT_SEEKABLE =
		" because it's in Ogg format, and it's not possible to seek Ogg streams due to the way Ogg works";
	private static final String FORMAT_CANT_SEEK = """
		It is not possible to seek the current track%s.""";

	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedVChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var track = getMusicManager(vc).getPlayingTrack();
		if (track == null)
			throw nothingIsPlaying(c);

		if (!track.isSeekable()) {
			String reason = "";
			if (isOgg(track))
				reason = OGG_NOT_SEEKABLE;
			else if (track.getInfo().isStream)
				reason = " because it's a stream";

			throw c.errorf(FORMAT_CANT_SEEK, DISABLED, reason);
		}

		long position = parseRelative(c, track);
		if (position == -1)
			position = parseAbsolute(c.params());

		if (position == -1)
			throw c.error(FORMAT_POSITION_FORMAT, FAILURE);

		if (track.getDuration() < position)
			throw c.error("That position is out of range", FAILURE);

		track.setPosition(position);
		c.react(ACCEPT_EMOJI);
	}

	private static long parseRelative(@Nonnull CommandContext c, @Nonnull AudioTrack track) {
		if (c.params().get(0).length() < 2)
			return -1;
		int multiplier = switch (c.params().get(0).charAt(0)) {
			case '+' -> 1;
			case '-' -> -1;
			case '±' -> throw c.error("?!");
			default -> 0;
		};
		if (multiplier == 0)
			return -1;
		long offset = parseRelativeTime(c.params().get(0).substring(1));
		if (offset == -1)
			return -1;
		else
			return max(0, track.getPosition() + offset * multiplier);
	}

	private static long parseAbsolute(@Nonnull Parameters p) {
		String[] times;
		if (p.get(0).contains(":")) {
			// hh:mm:ss
			times = p.get(0).split(":", 3);
		} else {
			// hh mm ss
			times = p.getArray();
		}

		try {
			long position = 0;
			for (int i = 0; i < times.length; i++)
				position += ABSOLUTE_UNITS[i + 3 - times.length].toMillis(Integer.parseInt(times[i]));
			return position;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static boolean isOgg(@Nonnull AudioTrack track) {
		return track instanceof HttpAudioTrack httpTrack
			&& httpTrack.getContainerTrackFactory().probe instanceof OggContainerProbe;
	}

	@Override
	public String getName() {
		return "seek";
	}

	@Override
	@SuppressWarnings("null")
	public String getInfo() {
		return format("""
			Seeks the currently playing track to the specified timestamp. \
			It is not possible to seek Ogg files due the way Ogg works.
			%s""", FORMAT_POSITION_FORMAT);
	}

	@Override
	public String[] getParameters() {
		return array("[position]");
	}

	@Override
	public int getMaxParameters() {
		return 3;
	}

	@Override
	public int getMinParameters() {
		return 1;
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
