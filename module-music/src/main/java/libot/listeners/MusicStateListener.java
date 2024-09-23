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
package libot.listeners;

import static libot.module.music.GlobalMusicManager.stopPlayback;
import static libot.utils.DiscordUtils.NO_BOT;

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MusicStateListener extends ListenerAdapter {

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		stopPlayback(event.getGuild().getIdLong());
	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		var channelLeft = event.getChannelLeft();
		if (channelLeft == null)
			return;

		var am = event.getGuild().getAudioManager();
		if (am.isConnected()) {
			if (event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
				am.closeAudioConnection();

			} else if (channelLeft.getMembers().stream().noneMatch(NO_BOT)) {
				stopPlayback(event.getGuild().getIdLong());
				am.closeAudioConnection();
			}
		}
	}

}
