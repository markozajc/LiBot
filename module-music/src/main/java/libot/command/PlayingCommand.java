//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
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

import static java.lang.Math.round;
import static libot.command.MusicCommandUtils.*;
import static libot.core.Constants.LITHIUM;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

import javax.annotation.Nonnull;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.module.music.GlobalMusicManager.MusicManager;

public class PlayingCommand extends Command {

	public PlayingCommand() {
		super(CommandMetadata.builder(MUSIC, "playing")
			.description("Displays detailed information about the currently playing track."));
	}

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
		e.setDescriptionf("""
			**`Title:   `** [%s](%s)
			**`Author:  `** %s
			**`Duration:`** %s
			```
			%s %s %s```""", escape(i.title, true), i.uri, escape(i.author, true), duration(track), playerState(manager),
						  progressBar(track), time(track));

		if (track.getSourceManager() instanceof YoutubeAudioSourceManager)
			e.setThumbnail("https://i.ytimg.com/vi/" + i.identifier + "/hq720.jpg");

		c.reply(e);
	}

	private static String playerState(@Nonnull MusicManager manager) {
		if (manager.getPlayer().isPaused())
			return EMOJI_PAUSE;
		else if (manager.getScheduler().isLoop())
			return EMOJI_LOOP;
		else
			return EMOJI_PLAY;
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

}
