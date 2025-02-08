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
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import libot.core.command.*;
import libot.core.entity.CommandContext;

public class LoopCommand extends Command {

	public LoopCommand() {
		super(CommandMetadata.builder(MUSIC, "loop")
			.requireDjRole(true)
			.description("Toggles looping state over the current track."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);

		if (manager.getScheduler().isLoop()) {
			manager.getScheduler().setLoop(false);
			c.reply("Loop OFF", "\u25B6 No longer looping.", SUCCESS);

		} else {
			var track = manager.getPlayingTrack();
			if (track == null)
				throw nothingIsPlaying(c);

			manager.getScheduler().setLoop(true);
			c.replyf("Loop ON", "\uD83D\uDD01 Now looping over **[%s](%s)**.", SUCCESS,
					 escape(track.getInfo().title).replace("]", "\\]"), track.getInfo().uri);
		}
	}

}
