package libot.core.listeners;

import static libot.core.music.GlobalMusicManager.stopPlayback;
import static libot.utils.DiscordUtils.NO_BOT;

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MusicRelatedListener extends ListenerAdapter {

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		var vc = event.getGuild().getAudioManager().getConnectedChannel();
		if (vc != null)
			stopPlayback(vc);
	}

	@Override
	public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
		var am = event.getGuild().getAudioManager();
		if (am.isConnected()) {
			if (event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
				am.closeAudioConnection();

			} else if (event.getChannelLeft().getMembers().stream().noneMatch(NO_BOT)) {
				stopPlayback(event.getChannelLeft());
				am.closeAudioConnection();
			}
		}
	}

}
