package libot.providers;

import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toCollection;
import static libot.module.music.GlobalMusicManager.*;
import static libot.module.music.TrackScheduler.QUEUE_MAX_SIZE;
import static libot.utils.DiscordUtils.NO_BOT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import javax.annotation.*;

import org.slf4j.Logger;

import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;

import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.shred.Shredder;
import libot.module.music.GlobalMusicManager.MusicManager;
import libot.providers.MusicRestoreProvider.MusicState;
import libot.utils.MessageLock;

public class MusicRestoreProvider extends SnowflakeProvider<MusicState> {

	private static final Logger LOG = getLogger(MusicRestoreProvider.class);

	public static record MusicState(@Nonnull String[] tracks, boolean paused, long position, boolean loop) {} // NOSONAR
	// [java:S6218] not needed

	public MusicRestoreProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "musicqueues");
	}

	@SuppressWarnings("null")
	private void restorePlayback(long vchannelId, @Nonnull MusicState state) {
		try {
			var vc = getShredder().getVoiceChannelById(vchannelId);
			if (vc == null || vc.getMembers().stream().noneMatch(NO_BOT))
				return;
			LOG.debug("Restoring playback on guild {}", vc.getGuild().getId());

			var am = vc.getGuild().getAudioManager();
			var manager = getMusicManager(vc);
			stopPlayback(vc);

			if (am.getSendingHandler() == null)
				am.setSendingHandler(manager.getSendHandler());

			if (!am.isConnected())
				am.openAudioConnection(vc);

			manager.getPlayer().setPaused(state.paused());
			manager.getScheduler().setLoop(state.loop());

			int skip = playFirst(state, manager);
			if (skip != -1)
				resolveAudioTracks(state.tracks(), skip).limit((long) QUEUE_MAX_SIZE - manager.getScheduler().size())
					.collect(toCollection(manager.getScheduler()::getQueue));
		} catch (Exception e) { // NOSONAR [java:S2142] the thread dies right after
			LOG.error("Could not restore audio playback on a guild", e);
		}
	}

	@SuppressWarnings("null")
	private static int playFirst(@Nonnull MusicState state, @Nonnull MusicManager manager) {
		int i = 0;
		if (manager.getPlayingTrack() == null) {
			AudioTrack firstTrack = null;
			while (firstTrack == null && i < state.tracks.length)
				firstTrack = resolveAudioTrack(state.tracks[i]);
			i++; // plus the one we just resolved

			if (firstTrack != null) {
				manager.getPlayer().playTrack(firstTrack);
				if (firstTrack.isSeekable() && firstTrack.getInfo().uri.equals(state.tracks()[0]))
					firstTrack.setPosition(state.position());
			} else {
				return -1; // we can't resolve a single track
			}
		}
		return i;
	}

	@Nullable
	private static AudioTrack resolveAudioTrack(@Nonnull String url) {
		var lock = new MessageLock<AudioTrack>();
		APM.loadItem(url, new AudioLoadResultHandler() {

			@Override
			public void trackLoaded(AudioTrack track) {
				lock.send(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				lock.send(null);
			}

			@Override
			public void noMatches() {
				lock.send(null);
			}

			@Override
			public void loadFailed(FriendlyException e) {
				LOG.warn("Couldn't resolve a track", e);
				lock.send(null);
			}
		});
		return lock.receive();
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Stream<AudioTrack> resolveAudioTracks(@Nonnull String[] urls, int skip) throws InterruptedException {
		var tracks = new AudioTrack[urls.length];
		var cdl = new CountDownLatch(urls.length - skip);
		for (int i = skip; i < urls.length; i++) {
			int j = i; // lambda shenanigans
			APM.loadItem(urls[i], new AudioLoadResultHandler() {

				@Override
				public void trackLoaded(AudioTrack track) {
					tracks[j] = track;
					cdl.countDown();
				}

				@Override
				public void playlistLoaded(AudioPlaylist playlist) {
					cdl.countDown();
				}

				@Override
				public void noMatches() {
					cdl.countDown();
				}

				@Override
				public void loadFailed(FriendlyException e) {
					LOG.warn("Couldn't resolve a track", e);
					cdl.countDown();
				}
			});
		}
		if (!cdl.await(5, MINUTES))
			LOG.warn("Timed out waiting for tracks, got {} out of {}", stream(tracks).filter(Objects::nonNull).count(),
					 urls.length);
		return stream(tracks).filter(Objects::nonNull);
	}

	@Override
	@SuppressWarnings("null")
	protected void onShredderReady() {
		if (!this.data.isEmpty()) {
			new Thread(() -> {
				this.data.forEach(this::restorePlayback);
				this.data.clear();
			}, "music-playback-restore").start();
		}
	}

	@Override
	public void shutdown() {
		getManagers().forEachKeyValue((guildId, manager) -> {
			var playing = manager.getPlayingTrack();
			if (playing == null)
				return;

			var queue = new ArrayList<>(manager.getScheduler().getQueue());
			int length = queue.size() + 1;
			if (length == 0)
				return;

			var tracks = new String[length];
			tracks[0] = playing.getInfo().uri;
			for (int i = 0; i < queue.size(); i++)
				tracks[i + 1] = queue.get(i).getInfo().uri;

			var state = new MusicState(tracks, manager.getPlayer().isPaused(), playing.getPosition(),
									   manager.getScheduler().isLoop());
			this.data.put(manager.getVchannelId(), state);
		});

		store();
	}

}
