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
import static libot.core.Constants.LITHIUM;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;

import javax.annotation.Nonnull;

import libot.core.command.*;
import libot.core.entity.CommandContext;

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
