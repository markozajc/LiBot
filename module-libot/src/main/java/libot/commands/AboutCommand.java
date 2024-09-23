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

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.time.Instant.ofEpochMilli;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.LIBOT;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.core.processes.ProcessManager;
import libot.module.music.GlobalMusicManager;
import libot.module.music.GlobalMusicManager.MusicManager;

public class AboutCommand extends Command {

	public AboutCommand() {
		super(CommandMetadata.builder(LIBOT, "about")
			.aliases("info", "botinfo", "stats")
			.description("Displays information about the bot."));
	}

	public static final String LINKS = """
		**[Get LiBot](https://libot.eu.org/get/)** - \
		**[Website](https://libot.eu.org/)** - \
		**[Support guild](https://discord.gg/asDUrbR)** - \
		**[Source](https://git.zajc.tel/libot/)**""";

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM);
		b.setTitle("About LiBot");
		b.setDescriptionf("""
			[LiBot](https://libot.eu.org/) is a Discord multi-purpose bot written by [Marko Zajc](https://zajc.tel/) in \
			[Java](https://openjdk.java.net/) using [JDA](https://github.com/DV8FromTheWorld/JDA/).
			%s""", LINKS);
		b.setThumbnail(c.getSelfUser().getAvatarUrl());
		appendStatistics(c, b);
		b.setFooterf("v%s | Last reboot ", VERSION);
		b.setTimestamp(ofEpochMilli(getRuntimeMXBean().getStartTime()));
		c.reply(b);
	}

	private static void appendStatistics(@Nonnull CommandContext c, @Nonnull EmbedPrebuilder b) {
		long playing = GlobalMusicManager.getManagers().values().stream().filter(MusicManager::isPlayingTrack).count();
		b.addFieldf("Statistics", """
			Guild count: **%d**,
			Total of **%d** commands launched,
			Currently playing music on **%d** guild%s.""", c.getShredder().getGuildCount(), ProcessManager.getCount(),
					playing, playing == 1 ? "" : "s", true);
	}

}
