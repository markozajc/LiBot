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

import static libot.command.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.SUCCESS;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.*;

import javax.annotation.Nonnull;

import libot.core.command.*;
import libot.core.entity.CommandContext;

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
