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
import static libot.core.Constants.*;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;

import libot.core.command.*;
import libot.core.entity.CommandContext;

public class ClearQueueCommand extends Command {

	public ClearQueueCommand() {
		super(CommandMetadata.builder(MUSIC, "clearqueue")
			.aliases("clear")
			.requireDjRole(true)
			.description("Clears the queue. The currently playing track will remain playing."));
	}

	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var scheduler = getMusicManager(vc).getScheduler();

		if (scheduler.isEmpty())
			throw c.error("The queue is already empty.", DISABLED);

		scheduler.clear();
		c.reply("Queue cleared successfully", SUCCESS);
	}

}
