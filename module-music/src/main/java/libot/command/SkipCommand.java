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

import static libot.command.MusicCommandUtils.*;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;

public class SkipCommand extends Command {

	@Nonnull private static final Parameter TRACKS =
		optional(POSITIONAL, "tracks", "number of tracks to skip, 1 by default");

	public SkipCommand() {
		super(CommandMetadata.builder(MUSIC, "skip")
			.requireDjRole(true)
			.parameters(TRACKS)
			.description("Skips to the next track in queue, stops playing if there are no more queued tracks."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		int amount = c.arg(TRACKS).map(Argument::valueAsInt).orElse(1);
		if (amount < 1)
			throw c.error("Can not skip backwards", FAILURE);

		var sched = getMusicManager(vc).getScheduler();
		var track = sched.skipTrack(amount);

		if (track != null)
			c.replyf(FORMAT_PLAY_TRACK, SUCCESS, sched.isLoop() ? EMOJI_LOOP : EMOJI_PLAY,
					 escape(track.getInfo().title, true), track.getInfo().uri, escape(track.getInfo().author, true));
		else
			c.react(ACCEPT_EMOJI);
	}

}
