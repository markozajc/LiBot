//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.command;

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.*;
import static java.util.regex.Pattern.compile;
import static libot.command.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static libot.util.ParseUtils.parseRelativeTime;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;

public class SeekCommand extends Command {

	@Nonnull private static final MandatoryParameter POSITION = mandatory(POSITIONAL, "position");

	public SeekCommand() {
		super(CommandMetadata.builder(MUSIC, "seek").requireDjRole(true).parameters(POSITION).description("""
			Seeks the currently playing track to the specified timestamp. \
			It is not possible to seek Ogg files due the way Ogg works.
			""" + POSITION_CHEATSHEET));
	}

	private static final Pattern TIME_DELIMITER = compile("[^\\d]+");
	private static final TimeUnit[] ABSOLUTE_UNITS = new TimeUnit[] { HOURS, MINUTES, SECONDS };
	private static final String POSITION_CHEATSHEET = """
		The position must be specified in a relative or absolute format, for example:
		Relative:
		 `+1s    ` seek forward one second
		 `-10m   ` seek backwards ten minutes
		 `+1h    ` seek forward one hour
		Absolute:
		 `1:00:00` seek to the first hour
		 `1:00   ` seek to the first minute
		 `1      ` seek to the first second
		 `0      ` seek to the beginning
		 (seeking to the beginning might cause certain tracks to stop playing)""";

	@SuppressWarnings("null")
	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var track = getMusicManager(vc).getPlayingTrack();
		if (track == null)
			throw nothingIsPlaying(c);

		if (!track.isSeekable()) {
			String reason = "";
			if (track.getInfo().isStream)
				reason = " because it's a stream";

			throw c.errorf("It is not possible to seek the current track%s.", DISABLED, reason);
		}

		var arg = c.arg(POSITION).value();
		long position = parseRelative(c, track, arg);
		if (position == -1)
			position = parseAbsolute(arg);

		if (position == -1)
			throw c.error(POSITION_CHEATSHEET, FAILURE);

		if (track.getDuration() < position)
			throw c.error("That position is out of range", FAILURE);

		track.setPosition(position);
		c.react(ACCEPT_EMOJI);
	}

	private static long parseRelative(@Nonnull CommandContext c, @Nonnull AudioTrack track, @Nonnull String arg) {
		if (arg.length() < 2)
			return -1;

		int multiplier = switch (arg.charAt(0)) {
			case '+' -> 1;
			case '-' -> -1;
			case '±' -> throw c.error("?!");
			default -> 0;
		};
		if (multiplier == 0)
			return -1;
		long offset = parseRelativeTime(arg.substring(1));
		if (offset == -1)
			return -1;
		else
			return max(0, track.getPosition() + offset * multiplier);
	}

	private static long parseAbsolute(@Nonnull String arg) {
		String[] times = TIME_DELIMITER.split(arg);

		try {
			long position = 0;
			for (int i = 0; i < times.length; i++)
				position += ABSOLUTE_UNITS[i + 3 - times.length].toMillis(Integer.parseInt(times[i]));
			return position;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

}
