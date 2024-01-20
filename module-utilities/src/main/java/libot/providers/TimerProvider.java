package libot.providers;

import static java.lang.Long.compare;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.TimedTaskProvider;
import libot.core.shred.Shredder;
import libot.providers.TimerProvider.UserTimer;

public class TimerProvider extends TimedTaskProvider<UserTimer> {

	public static record UserTimer(long userId, String text, long endTime) implements TimedTaskProvider.TimedTask {}

	public TimerProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "timers", "timer");
	}

	@Override
	@SuppressWarnings("null")
	public void onExpiry(UserTimer t) {
		getShredder().sendPrivateMessage(t.userId(), "**\u23F3 Timer:** %s".formatted(t.text()));
	}

	@Nonnull
	@SuppressWarnings("null")
	public List<UserTimer> getTimers(long userId) {
		return this.data.stream()
			.filter(t -> t.userId() == userId)
			.sorted((t1, t2) -> compare(t1.endTime(), t2.endTime()))
			.toList();
	}

}
