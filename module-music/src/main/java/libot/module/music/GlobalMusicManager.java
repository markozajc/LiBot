package libot.module.music;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry.DEFAULT_REGISTRY;
import static java.lang.System.getenv;
import static libot.core.Constants.*;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.*;

import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.*;
import org.slf4j.Logger;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.VoiceChannel;

public class GlobalMusicManager {

	private static final Logger LOG = getLogger(GlobalMusicManager.class);

	public static class MusicManager {

		@Nonnull private final AudioPlayer player;
		@Nonnull private final TrackScheduler scheduler;
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

		public boolean isPlayingTrack() {
			return getPlayingTrack() != null;
		}

	}

	private static final MutableLongObjectMap<MusicManager> MANAGERS =
		LongObjectMaps.mutable.<MusicManager>empty().asSynchronized();
	public static final AudioPlayerManager APM;
	static {
		APM = new DefaultAudioPlayerManager();
		if (getenv(ENV_YOUTUBE_EMAIL) == null || getenv(ENV_YOUTUBE_PASSWORD) == null) {
			LOG.warn("{} and/or {} are not set, age-restricted video playback will be unavailable", ENV_YOUTUBE_EMAIL,
					 ENV_YOUTUBE_PASSWORD);
		}
		APM.registerSourceManager(new YoutubeAudioSourceManager(true, getenv(ENV_YOUTUBE_EMAIL),
																getenv(ENV_YOUTUBE_PASSWORD)));
		APM.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
		APM.registerSourceManager(new BandcampAudioSourceManager());
		APM.registerSourceManager(new VimeoAudioSourceManager());
		APM.registerSourceManager(new TwitchStreamAudioSourceManager());
		APM.registerSourceManager(new BeamAudioSourceManager());
		APM.registerSourceManager(new GetyarnAudioSourceManager());
		APM.registerSourceManager(new HttpAudioSourceManager(DEFAULT_REGISTRY));
	}

	@Nonnull
	@SuppressWarnings("null")
	public static LongObjectMap<MusicManager> getManagers() {
		return MANAGERS;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static synchronized MusicManager getMusicManager(@Nonnull VoiceChannel vc) {
		return MANAGERS.getIfAbsentPut(vc.getGuild().getIdLong(), () -> new MusicManager(APM, vc.getIdLong()));
	}

	@Nullable
	public static MusicManager getMusicManager(long guildId) {
		return MANAGERS.get(guildId);
	}

	public static void stopPlayback(long guildId) {
		MusicManager gmm;
		if ((gmm = MANAGERS.get(guildId)) != null) {
			gmm.player.stopTrack();
			gmm.scheduler.clear();
		}
	}

	private GlobalMusicManager() {}

}
