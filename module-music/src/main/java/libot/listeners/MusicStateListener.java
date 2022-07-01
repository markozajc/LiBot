package libot.listeners;

import static libot.module.music.GlobalMusicManager.stopPlayback;
import static libot.utils.DiscordUtils.NO_BOT;

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MusicStateListener extends ListenerAdapter {

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		stopPlayback(event.getGuild().getIdLong());
	}

	@Override
	public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
		var am = event.getGuild().getAudioManager();
		if (am.isConnected()) {
			if (event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
				am.closeAudioConnection();

			} else if (event.getChannelLeft().getMembers().stream().noneMatch(NO_BOT)) {
				stopPlayback(event.getGuild().getIdLong());
				am.closeAudioConnection();
			}
		}
	}

}
