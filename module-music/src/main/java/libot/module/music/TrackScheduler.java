package libot.module.music;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import javax.annotation.*;

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {

	public static final int QUEUE_MAX_SIZE = 200;

	@Nonnull private final AudioPlayer player;
	@Nonnull private Queue<AudioTrack> queue;
	private boolean loop;
	private final transient AtomicInteger counter = new AtomicInteger();
	private final transient MutableIntObjectMap<Consumer<FriendlyException>> exceptionListeners =
		IntObjectMaps.mutable.<Consumer<FriendlyException>>empty().asSynchronized();
	private final transient MutableIntObjectMap<Supplier<Consumer<FriendlyException>>> playListeners =
		IntObjectMaps.mutable.<Supplier<Consumer<FriendlyException>>>empty().asSynchronized(); // lavaplayer is dumb

	/**
	 * @param player
	 *            The audio player this scheduler uses
	 */
	public TrackScheduler(@Nonnull AudioPlayer player) {
		this.player = player;
		this.queue = new ArrayBlockingQueue<>(QUEUE_MAX_SIZE);
		this.loop = false;

	}

	public void resumePlayer() {
		if (this.player.isPaused())
			this.player.setPaused(false);
	}

	public void queueCallback(@Nonnull AudioTrack track, @Nonnull Supplier<Consumer<FriendlyException>> playCallback,
							  @Nonnull Runnable queueCallback, @Nonnull Runnable queueFullCallback) {
		queueCallback(track, playCallback, queueCallback, queueFullCallback, true);
	}

	public void queueCallback(@Nonnull AudioTrack track, @Nonnull Supplier<Consumer<FriendlyException>> playCallback,
							  @Nonnull Runnable queueCallback, @Nonnull Runnable queueFullCallback, boolean resume) {
		int ticket = this.counter.getAndIncrement();
		track.setUserData(ticket);
		this.playListeners.put(ticket, playCallback);
		// this must be set **before** startTrack to avoid a race condition
		if (!this.player.startTrack(track, true)) {
			track.setUserData(null);
			this.playListeners.remove(ticket);
			if (this.queue.offer(track))
				queueCallback.run();
			else
				queueFullCallback.run();
		}

		if (resume)
			resumePlayer();
	}

	@Nullable
	public AudioTrack nextTrack() {
		return skipTrack(1);
	}

	@Nullable
	public AudioTrack skipTrack(int n) {
		for (int i = 0; i < n - 1; i++)
			this.queue.poll();

		AudioTrack track = this.queue.poll();
		resumePlayer();
		this.player.startTrack(track, false);
		return track;
	}

	public void shuffle() {
		List<AudioTrack> queueList = new ArrayList<>(this.queue);
		Collections.shuffle(queueList);
		clear();
		this.queue.addAll(queueList);
	}

	public void clear() {
		this.queue.clear();
	}

	public int size() {
		return this.queue.size();
	}

	public boolean isEmpty() {
		return this.queue.isEmpty();
	}

	public boolean isLoop() {
		return this.loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	public Queue<AudioTrack> getQueue() {
		return this.queue;
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		Object data = track.getUserData();
		if (data != null) {
			int ticket = (int) data;
			this.playListeners.remove(ticket);
			this.exceptionListeners.remove(ticket);
		}

		if (endReason.mayStartNext) {
			if (isLoop()) {
				AudioTrack current = track.makeClone();
				current.setPosition(0);
				player.startTrack(current, false);
			} else {
				nextTrack();
			}
		}
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		Object data = track.getUserData();
		if (data != null) {
			int ticket = (int) data;
			this.exceptionListeners.put(ticket, this.playListeners.get(ticket).get());
			this.playListeners.remove(ticket);
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		Object data = track.getUserData();
		if (data != null) {
			int ticket = (int) data;

			var exceptionListener = this.exceptionListeners.get(ticket);
			if (exceptionListener != null) {
				exceptionListener.accept(exception);
				this.exceptionListeners.remove(ticket);
			}
		}
	}

}
