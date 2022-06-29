package libot.providers;

import static java.lang.Long.compare;
import static java.util.function.Predicate.not;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.gson.reflect.TypeToken;

import libot.commands.PollCommand.Poll;
import libot.commands.PollCommand.Poll.PollException;
import libot.core.data.DataManager;
import libot.core.data.providers.TimedTaskProvider;
import libot.core.shred.Shredder;

public class PollProvider extends TimedTaskProvider<Poll> {

	private static final Logger LOG = getLogger(PollProvider.class);

	public PollProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "polls", "poll");
	}

	@Override
	public void onExpiry(Poll p) {
		try {
			p.end(getShredder(), false);
		} catch (PollException e) {
			LOG.error("Caught a PollException: {}", e.getReason());
		} catch (Exception e) {
			LOG.error("Caught an exception while submitting a poll", e);
		}
	}

	public List<Poll> getPolls(long guildId) {
		return this.data.stream()
			.filter(p -> p.getGuildId() == guildId)
			.filter(not(Poll::isDone))
			.sorted((p1, p2) -> compare(p1.endTime(), p2.endTime()))
			.toList();
	}

}
