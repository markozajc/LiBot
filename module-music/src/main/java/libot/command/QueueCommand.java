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

import static java.lang.String.format;
import static libot.command.MusicCommandUtils.*;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.entities.MessageEmbed.DESCRIPTION_MAX_LENGTH;

import java.util.*;

import javax.annotation.Nonnull;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.module.music.GlobalMusicManager.MusicManager;

public class QueueCommand extends Command {

	@Nonnull private static final Parameter PAGE = optional(POSITIONAL, "page");

	public QueueCommand() {
		super(CommandMetadata.builder(MUSIC, "queue")
			.parameters(PAGE)
			.description("Displays the current playing queue."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);
		var current = manager.getPlayingTrack();
		if (current == null)
			throw nothingIsPlaying(c);

		int page = c.arg(PAGE).map(Argument::valueAsInt).orElse(1);
		var pages = buildQueuePages(manager, current);

		if (page > pages.size() || page < 1)
			throw c.error("Page index out of range", FAILURE);

		String footer = null;
		if (pages.size() > 1) {
			footer = "Displaying page %d out of %d".formatted(page, pages.size());
			if (c.arg(PAGE).isEmpty())
				footer += format(" • run %s [page]", c.getCommandWithPrefix());
		}
		c.reply("Queue for " + c.getGuildName(), pages.get(page - 1).toString(), footer, LITHIUM);
	}

	@Nonnull
	private static List<StringBuilder> buildQueuePages(@Nonnull MusicManager gmm, @Nonnull AudioTrack current) {
		var list = new ArrayList<StringBuilder>();
		var b = new StringBuilder();

		b.append(EMOJI_PLAY + " **[%s: %s](%s)**\n".formatted(current.getInfo().author, current.getInfo().title,
															  current.getInfo().uri));

		int i = 1;
		for (var track : gmm.getScheduler().getQueue()) {
			var info = "**#%d** [%s: %s](%s)\n".formatted(i, track.getInfo().author, track.getInfo().title,
														  track.getInfo().uri);
			if (b.length() + info.length() > DESCRIPTION_MAX_LENGTH) {
				list.add(b);
				b = new StringBuilder();
			}

			b.append(info);
			i++;
		}

		list.add(b);
		return list;
	}

}
