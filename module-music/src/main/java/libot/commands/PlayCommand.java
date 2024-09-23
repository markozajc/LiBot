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
package libot.commands;

import static libot.commands.MusicCommandUtils.playUrl;
import static libot.commands.PauseCommand.RESUMED;
import static libot.core.Constants.LITHIUM;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.argument.UsageException;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class PlayCommand extends Command {

	@Nonnull private static final Parameter URL = optional(POSITIONAL, "url");

	public PlayCommand() {
		super(CommandMetadata.builder(MUSIC, "play")
			.requireDjRole(true)
			.parameters(URL)
			.description("Plays audio from a provided URL. Resumes playback if no URL is provided."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.arg(URL).map(Argument::value).ifPresentOrElse(url -> {
			playUrl(c, url);

		}, () -> {
			var manager = getMusicManager(c.getGuildIdLong());
			if (manager == null || !manager.getPlayer().isPaused())
				throw new UsageException("Missing argument: url");

			manager.getPlayer().setPaused(false);
			c.reply(RESUMED, LITHIUM);
		});
	}

}
