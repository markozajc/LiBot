package libot.core.music;

import static com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers.registerRemoteSources;
import static java.lang.System.getenv;

import javax.annotation.*;

import org.eclipse.collections.api.map.primitive.*;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import libot.core.Constants;
import libot.core.music.entities.TrackScheduler;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class GlobalMusicManager {

	public static class MusicManager {

		@Nonnull
		private final AudioPlayer player;
		@Nonnull
		private final TrackScheduler scheduler;
		private final long vchannelId;

		@SuppressWarnings("null")
		public MusicManager(@Nonnull AudioPlayerManager manager, long vchannelId) {
			this.player = manager.createPlayer();
			this.scheduler = new TrackScheduler(this.player);
			this.player.addListener(this.scheduler);
			this.vchannelId = vchannelId;
		}

		@Nonnull
		public AudioPlayer getPlayer() {
			return this.player;
		}

		@Nonnull
		public TrackScheduler getScheduler() {
			return this.scheduler;
		}

		public long getVchannelId() {
			return this.vchannelId;
		}

		@Nonnull
		public AudioPlayerSendHandler getSendHandler() {
			return new AudioPlayerSendHandler(this.player);
		}

		@Nullable
		public AudioTrack getPlayingTrack() {
			return getPlayer().getPlayingTrack();
		}

	}

	private static final MutableLongObjectMap<MusicManager> MUSIC_MANAGERS = LongObjectMaps.mutable.empty();
	public static final AudioPlayerManager AUDIO_PLAYER_MANAGER;
	static {
		AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
		YoutubeHttpContextFilter.setPAPISID(getenv(Constants.ENV_YOUTUBE_PAPISID));
		YoutubeHttpContextFilter.setPSID(getenv(Constants.ENV_YOUTUBE_PSID));
		registerRemoteSources(AUDIO_PLAYER_MANAGER);
	}

	@Nonnull
	@SuppressWarnings("null")
	public static LongObjectMap<MusicManager> getMusicManagers() {
		return MUSIC_MANAGERS;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static synchronized MusicManager getMusicManager(@Nonnull VoiceChannel vc) {
		return MUSIC_MANAGERS.getIfAbsentPut(vc.getGuild().getIdLong(),
											 () -> new MusicManager(AUDIO_PLAYER_MANAGER, vc.getIdLong()));
	}

	@Nullable
	public static MusicManager getMusicManager(long guildId) {
		return MUSIC_MANAGERS.get(guildId);
	}

	public static void stopPlayback(@Nonnull VoiceChannel vc) {
		MusicManager gmm;
		if ((gmm = getMusicManager(vc.getGuild().getIdLong())) != null) {
			gmm.player.stopTrack();
			gmm.scheduler.clear();
		}
	}

	private GlobalMusicManager() {}

}
