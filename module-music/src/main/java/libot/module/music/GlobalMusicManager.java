package libot.module.music;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry.DEFAULT_REGISTRY;

import javax.annotation.*;

import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.*;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

public class GlobalMusicManager {

	public static class MusicManager {

		@Nonnull private final AudioPlayer player;
		@Nonnull private final TrackScheduler scheduler;
		private final long channelId;
		@Nonnull private final ChannelType channelType;

		@SuppressWarnings("null")
		public MusicManager(@Nonnull AudioPlayerManager manager, @Nonnull AudioChannelUnion channel) {
			this.player = manager.createPlayer();
			this.scheduler = new TrackScheduler(this.player);
			this.player.addListener(this.scheduler);
			this.channelId = channel.getIdLong();
			this.channelType = channel.getType();
		}

		@Nonnull
		public AudioPlayer getPlayer() {
			return this.player;
		}

		@Nonnull
		public TrackScheduler getScheduler() {
			return this.scheduler;
		}

		public long getChannelId() {
			return this.channelId;
		}

		@Nonnull
		public ChannelType getChannelType() {
			return this.channelType;
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
		APM.registerSourceManager(new YoutubeAudioSourceManager(true, new MusicWithThumbnail(), new WebWithThumbnail(),
																new AndroidTestsuiteWithThumbnail(),
																new TvHtml5EmbeddedWithThumbnail()));
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
	public static synchronized MusicManager getMusicManager(@Nonnull AudioChannelUnion ac) {
		return MANAGERS.getIfAbsentPut(ac.getGuild().getIdLong(), () -> new MusicManager(APM, ac));
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
