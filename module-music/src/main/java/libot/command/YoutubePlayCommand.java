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
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.MUSIC;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;

public class YoutubePlayCommand extends Command {

	@Nonnull private static final MandatoryParameter QUERY = mandatory(POSITIONAL, "query", "the search query");

	public YoutubePlayCommand() {
		super(CommandMetadata.builder(MUSIC, "youtubeplay")
			.aliases("ytplay", "youtube", "yt")
			.requireDjRole(true)
			.parameters(QUERY)
			.description("""
				Searches for and plays a track from [YouTube](https://youtube.com). If you already know the URL, \
				use the 'play' command"""));
	}

	@SuppressWarnings("null")
	@Override
	public void execute(CommandContext c) {
		var selected = selectTrack(c, youtubeSearch(c.arg(QUERY).value()));
		playUrl(c, YOUTUBE_URL_PREFIX + selected.getIdentifier());
	}

}
