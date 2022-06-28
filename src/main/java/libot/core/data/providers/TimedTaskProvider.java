package libot.core.data.providers;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.*;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.*;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.TimedTaskProvider.TimedTask;
import libot.core.shred.Shredder;

public abstract class TimedTaskProvider<T extends TimedTask> extends Provider<Set<T>> {

	private static final Logger LOG = getLogger(TimedTaskProvider.class);
	private static final String FORMAT_SHUTDOWN = "This provider is shut down.";

	public interface TimedTask {

		long endTime();

	}

	private final ExecutorService expiryNotifier = newSingleThreadExecutor();
	private final Object mutex = new Object();
	private final String taskName;
	private boolean isShutdown;
	private Thread timerThread;

	protected TimedTaskProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager,
								@Nonnull TypeToken<Set<T>> typeToken, @Nonnull String dataKey,
								@Nonnull String taskName) {
		super(shredder, dataManager, typeToken, dataKey);
		this.taskName = taskName;
	}

	public void restartService() {
		interruptService();
		this.timerThread = new Thread(this::timerManager, this.taskName + "-service-thread");
		this.timerThread.start();
	}

	public boolean interruptService() {
		if (this.timerThread == null || this.timerThread.isInterrupted())
			return false;
		this.timerThread.interrupt();
		return true;
	}

	private void timerManager() {
		LOG.debug("Starting {} timer service thread", this.taskName);
		var timers = new HashSet<>(this.data);
		while (!timers.isEmpty()) {
			long now = currentTimeMillis();
			long nextTime = timers.stream().mapToLong(TimedTask::endTime).map(t -> t - now).min().orElse(0);

			if (nextTime > 0) {
				try {
					sleep(nextTime);
				} catch (InterruptedException e) {
					currentThread().interrupt();
					return;
				}
			}

			timers.removeIf(t -> {
				boolean expired = now > t.endTime();
				if (now > t.endTime()) {
					this.data.remove(t);
					this.expiryNotifier.submit(() -> onExpiry(t));
				}
				return expired;
			});
		}
	}

	public abstract void onExpiry(@Nonnull T task);

	public void register(@Nonnull T task) {
		synchronized (this.mutex) {
			if (this.isShutdown)
				throw new IllegalStateException(FORMAT_SHUTDOWN);

			this.data.add(task);
			restartService();
		}
	}

	public boolean deregister(@Nonnull T task) {
		synchronized (this.mutex) {
			if (this.isShutdown)
				throw new IllegalStateException(FORMAT_SHUTDOWN);

			boolean removed = this.data.remove(task);
			restartService();
			return removed;
		}
	}

	@Override
	public void shutdown() {
		synchronized (this.mutex) {
			interruptService();
			this.expiryNotifier.shutdown();
			try {
				this.expiryNotifier.awaitTermination(2, MINUTES);
			} catch (InterruptedException e) {
				LOG.warn("Got interrupted trying to shut down.");
				currentThread().interrupt();
				// complete remaining tasks regardless to not leave the service in an unstable state
			}
			this.isShutdown = true;
			super.shutdown();
		}
	}

	@Override
	protected void onShredderReady() {
		restartService();
	}

	@Override
	protected Set<T> createEmptyData() {
		return new HashSet<>();
	}

	@Override
	protected Set<T> constructData(String json) {
		return new HashSet<>(super.constructData(json));
	}

}
