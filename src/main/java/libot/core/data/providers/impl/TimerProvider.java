package libot.core.data.providers.impl;

import static java.lang.Long.compare;
import static java.lang.String.format;
import static libot.core.Constants.PRIVATE_MESSAGE_ERROR_HANDLER;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.TimedTaskProvider;
import libot.core.data.providers.impl.TimerProvider.UserTimer;
import libot.core.shred.Shredder;

public class TimerProvider extends TimedTaskProvider<UserTimer> {

	private static final String FORMAT_MESSAGE = "**\u23F3 Timer:** %s";

	public static record UserTimer(long userId, String text, long endTime) implements TimedTaskProvider.TimedTask {}

	public TimerProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "timers", "timer");
	}

	@Override
	@SuppressWarnings("null")
	public void onExpiry(UserTimer t) {
		var pc = getShredder().openPrivateChannelById(t.userId());
		if (pc != null)
			pc.flatMap(c -> c.sendMessage(format(FORMAT_MESSAGE, t.text()))).queue(null, PRIVATE_MESSAGE_ERROR_HANDLER);
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
